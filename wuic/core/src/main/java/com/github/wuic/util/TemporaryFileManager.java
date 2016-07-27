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

import com.github.wuic.exception.WuicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * <p>
 * This class manages temporary file creation relatively to a particular directory and make sure they are deleted when
 * the JVM shutdowns or after a time limit. The configured directory is already clean when this instance a new instance
 * of this class is created.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public final class TemporaryFileManager extends Thread {

    /**
     * The logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The directory containing temporary files.
     */
    private final File directory;

    /**
     * The time to live of each new file.
     */
    private final int timeToLiveSeconds;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param temporaryDirectory the base directory
     * @param timeToLiveSeconds the time to leave for created files
     */
    public TemporaryFileManager(final File temporaryDirectory, final int timeToLiveSeconds) {
        this.directory = temporaryDirectory;
        this.timeToLiveSeconds = timeToLiveSeconds;
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void start() {
        logger.info("Cleaning directory {}.", directory.getAbsolutePath());
        logger.info("Time to live to temporary files is {} seconds.", timeToLiveSeconds);

        // Clean directory if file exists
        if (directory.exists()) {
            if (directory.isFile()) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(
                        String.format("%s is not a directory.", directory.getAbsolutePath())));
            } else {
                IOUtils.delete(directory);

                if (directory.exists()) {
                    WuicException.throwBadArgumentException(new IllegalArgumentException(
                            String.format("Unable to delete content of directory %s.", directory.getAbsolutePath())));
                }
            }
        }
    }

    /**
     * <p>
     * Creates a new temporary file.
     * </p>
     *
     * @param parent parent pah withing the temporary directory
     * @param prefix the file prefix
     * @param suffix the file suffix
     * @return the new file
     * @throws IOException if any I/O occurs
     */
    public File createTempFile(final String parent, final String prefix, final String suffix) throws IOException {
        return createTempFile(parent, prefix, suffix, timeToLiveSeconds);
    }

    /**
     * <p>
     * Creates a new temporary file with a specific time to live.
     * </p>
     *
     * @param parent parent pah withing the temporary directory
     * @param prefix the file prefix
     * @param suffix the file suffix
     * @param ttl time to live in seconds for the returned file object
     * @return the new file
     * @throws IOException if any I/O occurs
     */
    public File createTempFile(final String parent, final String prefix, final String suffix, final int ttl) throws IOException {
        final File dir = new File(directory, parent);

        if (!dir.exists() && !dir.mkdirs()) {
            logger.warn("Unable to create directory for path {}.", dir.getAbsolutePath());
        }

        final File file = File.createTempFile(prefix, suffix, dir);

        if (ttl > 0) {
            WuicScheduledThreadPool.INSTANCE.execute(new Runnable() {
                @Override
                public void run() {
                    if (!file.delete()) {
                        logger.warn("Unable to delete file", file.getAbsolutePath());
                    }
                }
            }, ttl);
        }

        return file;
    }

    /**
     * <p>
     * Initializes this instance by cleaning the temporary directory.
     * </p>
     */
    private void init() {
        start();

        if (!directory.mkdirs()) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(
                    String.format("Unable to create %s directory.", directory.getAbsolutePath())));
        }

        Runtime.getRuntime().addShutdownHook(this);
    }
}
