/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.sample.polling.servlet;

import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.IOUtils;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;

/**
 * <p>
 * Creates a file on the disk which will be loaded by WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public class InitJavascriptFileListener implements ServletContextListener {

    /**
     * Directory where the file is stored.
     */
    public static final String DIRECTORY_PATH = IOUtils.mergePath(System.getProperty("java.io.tmpdir"), "wuic-polling");

    /**
     * File name.
     */
    public static final String FILE_NAME = "nut-polling-sample.js";

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        InputStream is = null;
        OutputStream os = null;

        final File dir = new File(DIRECTORY_PATH);
        if (dir.mkdirs()) {
            throw new IllegalStateException("Can't initialize sample : unable to create " + DIRECTORY_PATH);
        }

        // Prepare nut
        System.setProperty("wuic.dir", DIRECTORY_PATH);

        try {
            is = getClass().getResourceAsStream("/default-apps.js");
            os = new FileOutputStream(new File(dir, FILE_NAME));
            IOUtils.copyStream(is, os);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } catch (StreamException se) {
            throw new IllegalStateException(se);
        } finally {
            IOUtils.close(is, os);
        }

        // Prepare wuic.xml
        try {
            final File file = new File(dir, "wuic.xml");
            System.setProperty("wuic.xml", file.toURI().toURL().toString());
            is = getClass().getResourceAsStream("/default-wuic.xml");
            os = new FileOutputStream(file);
            IOUtils.copyStream(is, os);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } catch (StreamException se) {
            throw new IllegalStateException(se);
        } finally {
            IOUtils.close(is, os);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
    }
}
