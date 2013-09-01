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


package com.github.wuic.nut.test;

import com.github.wuic.ContextBuilder;
import com.github.wuic.engine.EngineBuilderFactory;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.xml.WuicXmlContextBuilderConfigurator;
import junit.framework.Assert;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
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
public class FtpTest {

    /**
     * The FTP server.
     */
    private static FtpServer server;

    /**
     * <p>
     * Starts the server.
     * </p>
     *
     * @throws FtpException if start operation fails
     */
    @BeforeClass
    public static void tearUp() throws FtpException, IOException {
        final FtpServerFactory serverFactory = new FtpServerFactory();

        // Configure user manager
        final PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setFile(File.createTempFile("wuic-nut-test", ".properties"));
        userManagerFactory.setPasswordEncryptor(new SaltedPasswordEncryptor());
        final UserManager um = userManagerFactory.createUserManager();

        // Configure user
        final BaseUser user = new BaseUser();
        user.setName("wuicuser");
        user.setPassword("wuicpassword");
        user.setHomeDirectory(FtpTest.class.getResource("/").getFile());
        um.save(user);

        // Configure port
        final ListenerFactory factory = new ListenerFactory();
        factory.setPort(2221);

        // Configure factory
        serverFactory.setUserManager(um);
        serverFactory.addListener("default", factory.createListener());
        server = serverFactory.createServer();
        server.start();
    }

    /**
     * <p>
     * Stops the server
     * </p>
     *
     * @throws FtpException if stop operation fails
     */
    @AfterClass
    public static void tearDown() throws FtpException {
        server.stop();
    }

    /**
     * <p>
     * Tests the FTP access resources with an embedded server.
     * </p>
     */
    @Test
    public void ftpTest() throws Exception {
        final ContextBuilder builder = new ContextBuilder();
        EngineBuilderFactory.getInstance().newContextBuilderConfigurator().configure(builder);
        new WuicXmlContextBuilderConfigurator(getClass().getResource("/wuic.xml")).configure(builder);
        final List<Nut> group = builder.build().process("css-image", "");

        Assert.assertFalse(group.isEmpty());
        InputStream is;

        for (Nut res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
        }

    }
}
