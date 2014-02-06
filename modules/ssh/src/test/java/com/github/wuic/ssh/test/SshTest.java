/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.Context;
import com.github.wuic.ContextBuilder;
import com.github.wuic.engine.EngineBuilderFactory;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.xml.FileXmlContextBuilderConfigurator;
import com.jcraft.jsch.JSchException;
import junit.framework.Assert;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Tests for FTP module.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.4
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
    public static void tearUp() throws StreamException, IOException {

        // Default server on port 9876
        sshdServer = SshServer.setUpDefaultServer();
        sshdServer.setPort(9876);

        // Host key
        final File hostKey = File.createTempFile("hostkey" + System.nanoTime(), ".ser");
        sshdServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKey.getAbsolutePath()));

        // Use cmd on windows, /bin/sh otherwise
/*        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            sshdServer.setShellFactory(new ProcessShellFactory(new String[]{ "cmd" }));
        } else {
            sshdServer.setShellFactory(new ProcessShellFactory(new String[]{ "/bin/sh", "-i", "-l" }));
        }
  */
        // Mock several additional configurations
        final SShMockConfig mock = new SShMockConfig();
        sshdServer.setCommandFactory(mock);
        sshdServer.setPasswordAuthenticator(mock);
        sshdServer.setPublickeyAuthenticator(mock);

        /*List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
        userAuthFactories.add(new UserAuthNone.Factory());
        sshd.setUserAuthFactories(userAuthFactories);
        sshd.setPublickeyAuthenticator(new PublickeyAuthenticator());


        sshdServer.setCommandFactory(new ScpCommandFactory());
         */
        List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
        namedFactoryList.add(new SftpSubsystem.Factory());
        sshdServer.setSubsystemFactories(namedFactoryList);


        // Run server
        sshdServer.start();

        // Copy nuts
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
     * Tests the SSH access nuts with an embedded server.
     * </p>
     *
     * @throws JSchException if SSH session could not be opened
     * @throws WuicException if WUIC request fails
     * @throws InterruptedException if the SSH server does not respond in time
     * @throws IOException if any I/O error occurs
     * @throws JAXBException if test fails
     */
    @Test
    public void sshTest() throws JSchException, IOException, InterruptedException, WuicException, JAXBException {
        final ContextBuilder builder = new ContextBuilder();
        EngineBuilderFactory.getInstance().newContextBuilderConfigurator().configure(builder);
        new FileXmlContextBuilderConfigurator(getClass().getResource("/wuic.xml")).configure(builder);
        final Context facade = builder.build();
        final List<Nut> group = facade.process("", "css-imagecss-image");

        Assert.assertFalse(group.isEmpty());
        InputStream is;

        for (final Nut res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
        }
    }
}
