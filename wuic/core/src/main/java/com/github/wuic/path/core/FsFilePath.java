/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.path.core;

import com.github.wuic.path.DirectoryPath;
import com.github.wuic.path.FilePath;
import com.github.wuic.path.FsItem;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.Input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * <p>
 * This {@link com.github.wuic.path.FilePath} represents a path on the path system.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.4
 */
public class FsFilePath extends SimplePath implements FilePath, FsItem {

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
     * @param charset the charset
     * @param f the path
     * @param parent the parent, {@code null} if this path is a root
     */
    public FsFilePath(final File f, final DirectoryPath parent, final String charset) {
        super(f.getName(), parent, charset);

        if (!f.isFile()) {
            throw new IllegalArgumentException(String.format("%s is not a file on the file system", f.getAbsolutePath()));
        }

        file = f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        return new DefaultInput(new FileInputStream(file), getCharset());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastUpdate() {
        return file.lastModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getFile() {
        return file;
    }
}
