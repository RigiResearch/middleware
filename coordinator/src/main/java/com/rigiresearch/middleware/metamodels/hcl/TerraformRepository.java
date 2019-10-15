package com.rigiresearch.middleware.metamodels.hcl;

import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.Map;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Git repository containing Terraform templates.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class TerraformRepository {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(TerraformRepository.class);

    /**
     * A local Git repository.
     */
    private final Repository repository;

    /**
     * A repository credentials provider.
     */
    private final CredentialsProvider credentials;

    /**
     * Whether to skip the CI server.
     */
    private final boolean skipci;

    /**
     * Ah HCL parser.
     */
    private final HclParser parser;

    /**
     * A merge utility for HCL models.
     */
    private final HclMergeStrategy merger;

    /**
     * Default constructor.
     * @param remote The repository's remote URL
     * @param token An authentication token
     * @throws IOException If there's an error creating a temporal directory
     * @throws GitAPIException If there's a problem cloning the Git repository
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public TerraformRepository(final URIish remote, final String token)
        throws IOException, GitAPIException {
        this.credentials = new UsernamePasswordCredentialsProvider(token, "");
        this.repository = Git.cloneRepository()
            .setURI(remote.toString())
            .setDirectory(Files.createTempDirectory("").toFile())
            .setCredentialsProvider(this.credentials)
            .call()
            .getRepository();
        this.skipci = false;
        this.parser = new HclParser();
        this.merger = new HclMergeStrategy();
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(() -> this.repository.getDirectory()
                    .getParentFile()
                    .delete()
                )
            );
    }

    /**
     * Updates the Terraform templates in this directory based on the given model.
     * @param specification The specification that represents the current deployment
     * @throws IOException If there's an error opening the .git directory
     * @throws GitAPIException If there's an error pulling changes from the repository
     * @throws HclParsingException If there's a parsing error with the repo templates
     */
    public void update(final Specification specification)
        throws IOException, GitAPIException, HclParsingException {
        try (Git git = Git.open(this.repository.getDirectory())) {
            git.pull()
                .setStrategy(MergeStrategy.THEIRS)
                .setCredentialsProvider(this.credentials)
                .call();
            this.updateTemplates(specification);
            if (git.status().call().isClean()) {
                TerraformRepository.LOGGER.info("The repository is already up to date");
                return;
            }
            final Calendar calendar = Calendar.getInstance();
            final String branch = String.format(
                "update-%d/%d/%d-%d:%d:%d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            );
            git.branchCreate()
                .setName(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .call();
            this.addAndCommit(git);
            git.push()
                .add(branch)
                .setCredentialsProvider(this.credentials)
                .call();
            TerraformRepository.LOGGER.info("Pushed changes to remote repository");
            // TODO Create pull request
        }
    }

    /**
     * Updates the specification files.
     * @param specification The specification that represents the current deployment
     * @throws HclParsingException If there's a parsing error
     * @throws IOException If there's an I/O error
     */
    private void updateTemplates(final Specification specification)
        throws HclParsingException, IOException {
        final File directory = this.repository.getDirectory().getParentFile();
        final File[] templates = directory.listFiles(
            (file, name) -> !file.isDirectory() && name.endsWith(".tf")
        );
        if (templates == null || templates.length == 0) {
            // The resources are being imported for the first time
            Files.write(
                Paths.get(directory.getAbsolutePath(), "main.tf"),
                this.parser.parse(specification).getBytes(),
                StandardOpenOption.CREATE_NEW
            );
        } else {
            final SpecificationSet set = this.parser.parse(templates);
            set.update(this.merger.merge(set.unified(), specification));
            final Map<URI, String> source = this.parser.parse(set);
            for (final File template : templates) {
                template.delete();
            }
            for (final Map.Entry<URI, String> entry : source.entrySet()) {
                Files.write(
                    new File(directory, entry.getKey().toFileString()).toPath(),
                    entry.getValue().getBytes(),
                    StandardOpenOption.CREATE_NEW
                );
            }
        }
    }

    /**
     * Adds updated, removed and new files to the index and then commits the
     * changes.
     * @param git The git repository
     * @throws NoFilepatternException See {@link Git#add()}
     * @throws GitAPIException See {@link Git}
     */
    private void addAndCommit(final Git git)
        throws NoFilepatternException, GitAPIException {
        for (final String file : git.status().call().getModified()) {
            git.add().addFilepattern(file).call();
            this.commit(git, String.format("Update %s", file));
        }
        for (final String file : git.status().call().getMissing()) {
            git.rm().addFilepattern(file).call();
            this.commit(git, String.format("Delete %s", file));
        }
        for (final String file : git.status().call().getUntracked()) {
            git.add().addFilepattern(file).call();
            this.commit(git, String.format("Add %s", file));
        }
    }

    /**
     * Commits already added changes.
     * @param git The git repository
     * @param message The commit message
     * @throws GitAPIException See {@link Git}
     */
    private void commit(final Git git, final String message) throws GitAPIException {
        String host = "unknown";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException exception) {
            TerraformRepository.LOGGER.error("Error getting the hostname", exception);
        }
        final String text;
        if (this.skipci) {
            text = String.format("%s [skip ci]", message);
        } else {
            text = message;
        }
        git.commit()
            .setCommitter("coordinator", String.format("coordinator@%s", host))
            .setMessage(text)
            .call();
    }
}
