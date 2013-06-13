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


package com.github.wuic.ssh.test;

import com.github.wuic.WuicFacade;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.util.IOUtils;
import com.jcraft.jsch.JSchException;
import junit.framework.Assert;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.List;

/**
 * <p>
 * Tests for FTP module.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.1
 */
@RunWith(JUnit4.class)
public class SshTest {

    /**
     * SSH server.
     */
    private static SshServer sshdServer;

    /**
     * <p>
     * Starts the server.
     * </p>
     */
    @BeforeClass
    public static void tearUp() throws Exception {

        // Default server on port 9876
        sshdServer = SshServer.setUpDefaultServer();
        sshdServer.setPort(9876);

        // Host key
        final File hostKey = File.createTempFile("hostkey" + System.nanoTime(), ".ser");
        sshdServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKey.getAbsolutePath()));

        // Use cmd on windows, /bin/sh otherwise
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            sshdServer.setShellFactory(new ProcessShellFactory(new String[]{ "cmd" }));
        } else {
            sshdServer.setShellFactory(new ProcessShellFactory(new String[]{ "/bin/sh", "-i", "-l" }));
        }

        // Mock several additional configurations
        final SShMockConfig mock = new SShMockConfig();
        sshdServer.setCommandFactory(mock);
        sshdServer.setPasswordAuthenticator(mock);
        sshdServer.setPublickeyAuthenticator(mock);

        // Run server
        sshdServer.start();

        // Copy resources
        com.github.wuic.util.IOUtils.copyStream(new FileInputStream(new File(SshTest.class.getResource("/chosen.css").getFile())),
                new FileOutputStream(File.createTempFile("junit-chosen", ".css")));
        com.github.wuic.util.IOUtils.copyStream(new FileInputStream(new File(SshTest.class.getResource("/style.css").getFile())),
                new FileOutputStream(File.createTempFile("junit-style", ".css")));
    }

    /**
     * <p>
     * Stops the server
     * </p>
     *
     * @throws InterruptedException if the thread in charge of stopping the service is interrupted
     */
    @AfterClass
    public static void tearDown() throws InterruptedException {
        sshdServer.stop();
    }

    /**
     * <p>
     * Tests the SSH access resources with an embedded server.
     * </p>
     *
     * @throws Exception if SSH session could not be opened
     */
    @Test
    public void sshTest() throws Exception {
        final WuicFacade facade = WuicFacade.newInstance("");
        final List<WuicResource> group = facade.getGroup("css-image", "");

        Assert.assertFalse(group.isEmpty());
        InputStream is;

        for (WuicResource res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
        }
    }
}
