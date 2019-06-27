package com.rigiresearch.middleware.historian.templates;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A file visitor to copy a directory.
 * From <a href="https://stackoverflow.com/a/10068306/738968">here</a>.
 * @author Miguel Jimenez (miguel@uvic.ca)
 * @version $Id$
 * @since 0.1.0
 */
public final class CopyFileVisitor extends SimpleFileVisitor<Path> {

    /**
     * The target path.
     */
    private final Path target;

    /**
     * The configuration options.
     */
    private final CopyOption[] options;

    /**
     * The source path.
     */
    private Path source;

    /**
     * Default constructor.
     * @param target The target path
     * @param options The configuration options
     */
    public CopyFileVisitor(final Path target, final CopyOption... options) {
        this.target = target;
        this.options = options.clone();
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
        final BasicFileAttributes attrs) throws IOException {
        if (this.source == null) {
            this.source = dir;
        } else {
            Files.createDirectories(
                this.target.resolve(this.source.relativize(dir))
            );
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file,
        final BasicFileAttributes attrs) throws IOException {
        Files.copy(
            file,
            this.target.resolve(this.source.relativize(file)),
            this.options);
        return FileVisitResult.CONTINUE;
    }
}
