package com.rigiresearch.middleware.coordinator;

import com.rigiresearch.middleware.coordinator.templates.CamTemplates;
import com.rigiresearch.middleware.metamodels.hcl.HclMergeStrategy;
import com.rigiresearch.middleware.metamodels.hcl.Specification;
import com.rigiresearch.middleware.metamodels.hcl.SpecificationSet;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParser;
import com.rigiresearch.middleware.notations.hcl.parsing.HclParsingException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import lombok.Getter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
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
     * The current branch.
     */
    @Getter
    private Ref branch;

    /**
     * Default constructor.
     * @param remote The repository's remote URL
     * @param token An authentication token
     * @throws IOException If there's an error creating a temporal directory
     * @throws GitAPIException If there's a problem cloning the Git repository
     */
    public TerraformRepository(final URIish remote, final String token)
        throws IOException, GitAPIException {
        this.credentials = new UsernamePasswordCredentialsProvider(token, "");
        this.repository = this.initializeRepository(remote);
        this.skipci = false;
        this.parser = new HclParser();
        this.merger = new HclMergeStrategy();
    }

    /**
     * Clones a git repository and creates a shutdown hook to remove it.
     * @param remote The URL of the repository
     * @return A git repoitory
     * @throws IOException If there's an error creating a temporal directory
     * @throws GitAPIException If there's a problem cloning the Git repository
     */
    private Repository initializeRepository(final URIish remote)
        throws IOException, GitAPIException {
        final Repository repo = Git.cloneRepository()
            .setURI(remote.toString())
            .setDirectory(Files.createTempDirectory("").toFile())
            .setCredentialsProvider(this.credentials)
            .call()
            .getRepository();
        TerraformRepository.LOGGER.info("Cloned repository {}", remote.toString());
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(() -> {
                    try {
                        final Path path =
                            Paths.get(repo.getDirectory().getAbsolutePath());
                        Files.walk(path)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    } catch (final IOException exception) {
                        TerraformRepository.LOGGER.error(
                            "Error removing temporary directory",
                            exception
                        );
                    }
                    TerraformRepository.LOGGER.info("Removed local repository");
                })
            );
        return repo;
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
            this.prepareBranch(git);
            this.updateTemplates(specification);
            this.updateCamTemplate(
                git.getRepository().getDirectory().getParentFile(),
                specification
            );
            if (git.status().call().isClean()) {
                TerraformRepository.LOGGER.info("The repository is already up to date");
                return;
            }
            final Calendar calendar = Calendar.getInstance();
            final String name = String.format(
                "update/%d-%d-%d-%d_%d_%d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            );
            git.branchCreate()
                .setName(name)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .call();
            this.branch = git.checkout()
                .setName(name)
                .call();
            this.addAndCommit(git);
            git.push()
                .add(name)
                .setCredentialsProvider(this.credentials)
                .call();
            TerraformRepository.LOGGER.info("Pushed changes to remote repository");
            // TODO Create pull request
        }
    }

    /**
     * Updates or creates IBM CAM's templates.
     * @param directory The repository directory
     * @param specification The latest specification
     * @throws IOException If there's an I/O error
     */
    private void updateCamTemplate(final File directory,
        final Specification specification) throws IOException {
        final CamTemplates templates = new CamTemplates();
        final File template = new File(directory, "camtemplate.json");
        if (!template.exists()) {
            Files.write(
                template.toPath(),
                templates.template().getBytes(),
                StandardOpenOption.CREATE_NEW
            );
        }
        final File variables = new File(directory, "camvariables.json");
        if (!variables.exists()) {
            variables.createNewFile();
            // FIXME This should not create a plain file. It should create a Json
            //  object and use writeValueAsString to replace the whole file
            Files.write(
                variables.toPath(),
                templates.variables(specification).getBytes(),
                StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }

    /**
     * Prepares the repository and the branch to use depending on the state of
     * the repository and the remote branches.
     * @param git The git repository
     * @throws IOException If there's an I/O error
     * @throws GitAPIException If there's a git error
     */
    private void prepareBranch(final Git git) throws IOException, GitAPIException {
        if (this.branch == null) {
            final Ref head = git.getRepository().findRef(Constants.HEAD);
            if (head == null || head.getObjectId() == null) {
                // This is a new repository. We need to create a file to have a HEAD
                final File file =
                    new File(git.getRepository().getDirectory().getParentFile(), ".gitignore");
                file.createNewFile();
                this.addAndCommit(git);
            }
            final String current = git.getRepository().getBranch();
            this.branch = git.checkout().setName(current).call();
            TerraformRepository.LOGGER.debug("Checked out branch '{}'", current);
        }
        // First, fetch the latest changes from the remote repo
        git.fetch()
            .setCredentialsProvider(this.credentials)
            .setRemote(Constants.DEFAULT_REMOTE_NAME)
            .call();
        // Then, determine whether the current branch still exists
        final String shorthand = this.branch.getName().replace("refs/heads/", "");
        final boolean exists = git.branchList()
            .setListMode(ListBranchCommand.ListMode.REMOTE)
            .call()
            .stream()
            .anyMatch(ref -> ref.getName().contains(shorthand));
        if (exists) {
            // If it was created before, it hasn't been merged yet
            git.merge()
                .include(git.getRepository().findRef(Constants.FETCH_HEAD))
                .setStrategy(MergeStrategy.THEIRS)
                .call();
        } else {
            git.checkout().setName(Constants.MASTER).call();
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
            (file, name) -> !new File(file, name).isDirectory()
                && name.endsWith(".tf")
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
                TerraformRepository.LOGGER.debug("Removed local template {}", template);
            }
            for (final Map.Entry<URI, String> entry : source.entrySet()) {
                File template = new File(entry.getKey().toFileString());
                if (!template.isAbsolute()) {
                    template = new File(directory, entry.getKey().toFileString());
                }
                Files.write(
                    template.toPath(),
                    entry.getValue().getBytes(),
                    StandardOpenOption.CREATE_NEW
                );
                TerraformRepository.LOGGER.debug(
                    "Created template {}",
                    entry.getKey().toFileString()
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
        // Modified
        final Collection<String> modified =
            new ArrayList<>(git.status().call().getModified());
        modified.addAll(git.status().call().getChanged());
        for (final String file : modified) {
            git.add().addFilepattern(file).call();
            this.commit(git, String.format("Update %s", file));
        }
        // Deleted
        final Collection<String> deleted =
            new ArrayList<>(git.status().call().getMissing());
        deleted.addAll(git.status().call().getRemoved());
        for (final String file : deleted) {
            git.rm().addFilepattern(file).call();
            this.commit(git, String.format("Delete %s", file));
        }
        // Untracked
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
