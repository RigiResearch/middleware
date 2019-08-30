package com.rigiresearch.middleware.historian;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.FileConverter;
import java.io.File;
import java.util.Locale;
import java.util.Optional;
import lombok.Getter;

/**
 * Arguments configuration for the command line interface.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
final class CliArguments {

    /**
     * The jCommander instance.
     */
    private final JCommander jcommander;

    /**
     * The generate command.
     */
    @Getter
    private final CliArguments.GenerateCommand generate;

    /**
     * Display the help menu.
     */
    @Getter
    @Parameter(names = "--help", help = true)
    private boolean help;

    /**
     * Default constructor.
     */
    CliArguments() {
        this.generate = new CliArguments.GenerateCommand();
        this.jcommander = JCommander.newBuilder()
            .addObject(this)
            .addCommand("generate", this.generate)
            .build();
    }

    /**
     * Parses the application arguments.
     * @param args The application arguments
     */
    void parse(final String... args) {
        this.jcommander.setProgramName("Historian");
        this.jcommander.parse(args);
    }

    /**
     * Displays a help message.
     */
    void usage() {
        this.jcommander.usage();
    }

    /**
     * Displays a help message.
     * @param command The command for which the usage is displayed.
     */
    void usage(final Command command) {
        this.jcommander.usage(command.toString());
    }

    /**
     * The command to execute.
     * @return A {@link CliArguments.Command} constant.
     */
    Optional<CliArguments.Command> getParsedCommand() {
        final Optional<CliArguments.Command> optional;
        if (this.jcommander.getParsedCommand() == null) {
            optional = Optional.empty();
        } else {
            optional = Optional.of(
                CliArguments.Command.valueOf(
                    this.jcommander.getParsedCommand()
                        .toUpperCase(Locale.getDefault())
                )
            );
        }
        return optional;
    }

    /**
     * The action to perform.
     */
    public enum Command {
        /**
         * Generate code.
         */
        GENERATE;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.getDefault());
        }
    }

    /**
     * The artifact type.
     */
    public enum ArtifactType {
        /**
         * A dot specification.
         */
        DOT,

        /**
         * A gradle project.
         */
        PROJECT;

        @Override
        public String toString() {
            return this.name().toLowerCase(Locale.getDefault());
        }
    }

    /**
     * A command for generating source code.
     */
    @Getter
    @Parameters(separators = "=")
    static final class GenerateCommand {

        /**
         * The type of artifact to generate.
         */
        @Parameter(
            names = {"--type", "-t"},
            converter = OptionConverter.class,
            description = "The artifact to generate",
            required = true
        )
        private ArtifactType type;

        /**
         * The input file (i.e., an OpenAPI specification or a graph
         * configuration).
         */
        @Parameter(
            names = {"--input", "-i"},
            converter = FileConverter.class,
            description = "The input file (i.e., either an OpenAPI JSON "
                + "specification or a configuration graph)",
            required = true
        )
        private File input;

        /**
         * The output directory.
         */
        @Parameter(
            names = {"--output", "-o"},
            converter = FileConverter.class,
            description = "The path to a directory where the artifacts will be generated",
            required = true
        )
        private File output;

        /**
         * Display the help menu.
         */
        @Getter
        @Parameter(names = "--help", help = true)
        private boolean help;

    }

}
