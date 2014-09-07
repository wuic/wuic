package com.github.wuic.path.core;

import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.FilePath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * This {@link com.github.wuic.path.FilePath} represents a path on the path system.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.4
 */
public class FsFilePath extends SimplePath implements FilePath {

    /**
     * The path.
     */
    private File file;

    /**
     * <p>
     * Builds a new instance. Throws a {@link IllegalArgumentException} if the specified path does not represents a
     * path on the path system.
     * </p>
     *
     * @param f the path
     * @param parent the parent, {@code null} if this path is a root
     */
    public FsFilePath(final File f, final DirectoryPath parent) {
        super(f.getName(), parent);

        if (!f.isFile()) {
            throw new IllegalArgumentException(String.format("%s is not a file on the file system", f.getAbsolutePath()));
        }

        file = f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openStream() throws IOException {
        return new FileInputStream(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() {
        return file.lastModified();
    }
}
