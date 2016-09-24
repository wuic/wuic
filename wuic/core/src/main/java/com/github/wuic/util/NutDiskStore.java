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


package com.github.wuic.util;

import com.github.wuic.nut.ConvertibleNut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * <p>
 * This class stores nuts in a dedicated temporary directory. The class keeps all version numbers of nuts when they are
 * stored so the file is actually copied only if its number version is changed in order to boost performances. A thread
 * is registered via {@link Runtime#addShutdownHook(Thread)} to clean directory when JVM stops properly.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.1
 */
public enum NutDiskStore implements Runnable {

    /**
     * Singleton.
     */
    INSTANCE;

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Dedicated temporary directory.
     */
    private final File workingDirectory;

    /**
     * All nut names previously stored.
     */
    private final Map<String, Long> storedNuts;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    private NutDiskStore() {
        final String now = String.valueOf(System.currentTimeMillis());
        final String path = IOUtils.mergePath(System.getProperty("java.io.tmpdir"), getClass().getName(), now);
        workingDirectory = new File(path);
        workingDirectory.mkdirs();
        storedNuts = new HashMap<String, Long>();
        Runtime.getRuntime().addShutdownHook(new Thread(this));
    }

    /**
     * <p>
     * Gets the working directory.
     * </p>
     *
     * @return the working directory
     */
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * <p>
     * Creates a {@link File} for the given nut and returns the object created for the stored file.
     * {@code null} will be returned if the file already exists and has the same version number.
     * </p>
     *
     * @param nut the nut to store
     * @return the file
     * @throws IOException if file cannot be created
     * @throws InterruptedException if version number can't be retrieved
     * @throws ExecutionException if version number can't be retrieved
     */
    public File store(final ConvertibleNut nut) throws IOException, InterruptedException, ExecutionException {
        final File path = new File(getWorkingDirectory(), nut.getName());
        final Long version = storedNuts.get(nut.getName());
        final Long actual = nut.getVersionNumber().get();

        // Do not rewrite an up to date file
        if (version != null) {
            if (!version.equals(actual) && !path.delete()) {
                logger.warn(String.format("Unable to delete '%s'", path.getAbsolutePath()), new IOException());
            }
        } else {
            storedNuts.put(nut.getName(), actual);
            path.getParentFile().mkdirs();
        }

        return path;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        IOUtils.delete(workingDirectory);
    }
}