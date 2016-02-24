/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * -   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
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