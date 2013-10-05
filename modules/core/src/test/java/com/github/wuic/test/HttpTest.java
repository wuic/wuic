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


package com.github.wuic.test;

import com.github.wuic.Context;
import com.github.wuic.ContextBuilder;
import com.github.wuic.engine.EngineBuilderFactory;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.xml.WuicXmlContextBuilderConfigurator;
import junit.framework.Assert;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * <p>
 * HTTP tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.4
 * @since 0.3.1
 */
@RunWith(JUnit4.class)
public class HttpTest extends WuicTest {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Server instance.
     */
    private static Server SERVER;

    /**
     * <p>
     * Starts the servlet container.
     * </p>
     *
     * @throws Exception if server can't starts.
     */
    @BeforeClass
    public static void tearUp() throws Exception {
        // Create server with webapp
        SERVER = new Server(9876);

        // Do not forget to turn on HTTPS in wuic-http.xml to test HTTPS
        //activateSsl();

        SERVER.setHandler(new AbstractHandler() {
            public void handle(final String target,
                               final Request baseRequest,
                               final HttpServletRequest request,
                               final HttpServletResponse response)
                    throws IOException, ServletException {
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(Boolean.TRUE);
                try {
                    IOUtils.copyStream(HttpTest.class.getResourceAsStream(request.getRequestURI()), response.getOutputStream());
                } catch (StreamException wse) {
                    throw new IOException(wse);
                }
            }
        });

        SERVER.start();

        // Uncomment if you want to open URL manually in your browser
        //SERVER.join();
    }

    private static void activateSsl() {
        // Keystore to declare on both client and server side
        final String keyStore = HttpTest.class.getResource("/nonguestablepassword.keystore").getFile();

        // Client configuration
        System.setProperty("javax.net.ssl.trustStore", keyStore);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

        HttpsURLConnection.setDefaultHostnameVerifier(
                new HostnameVerifier() {
                    public boolean verify(final String hostname, final SSLSession sslSession) {
                        return hostname.equals("localhost");
                    }
                });


        // Server configuration
        final SslSocketConnector connector = new SslSocketConnector();
        connector.setPort(9876); //or here .it's up to you
        connector.setPassword("nonguestablepassword"); //password which you set during creation of certificate
        connector.setKeyPassword("nonguestablepassword"); //password which you set during creation of certificate
        connector.setKeystore(HttpTest.class.getResource("/nonguestablepassword.keystore").getFile()); // path to your keystroke path (depend what you've done in step 4(or 3 )
        connector.setTrustPassword("nonguestablepassword");
        SERVER.setConnectors(new Connector[] { connector });
    }

    /**
     * <p>
     * Stops the servlet container.
     * </p>
     *
     * @throws Exception if an error occurs while stopping
     */
    @AfterClass
    public static void tearDown() throws Exception {
        SERVER.stop();
    }

    /**
     * Test HTTP nuts.
     *
     * @throws Exception if test fails
     */
    @Test
    public void httpNutTest() throws Exception {
        Long startTime = System.currentTimeMillis();
        final ContextBuilder builder = new ContextBuilder();
        EngineBuilderFactory.getInstance().newContextBuilderConfigurator().configure(builder);
        new WuicXmlContextBuilderConfigurator(getClass().getResource("/wuic-http.xml")).configure(builder);
        final Context facade = builder.build();
        Long loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        startTime = System.currentTimeMillis();
        final List<Nut> group = facade.process("css-imagecss-image", "");
        loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        Assert.assertFalse(group.isEmpty());
        InputStream is;

        for (Nut res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
        }
    }
}
