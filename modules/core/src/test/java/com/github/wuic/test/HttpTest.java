////////////////////////////////////////////////////////////////////
//
// File: CoreTest.java
// Created: 18 July 2012 10:00:00
// Author: GDROUET
// Copyright C 2012 Capgemini.
//
// All rights reserved.
//
////////////////////////////////////////////////////////////////////


package com.github.wuic.test;

import com.github.wuic.WuicFacade;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.util.IOUtils;
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
 * @version 1.1
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
                IOUtils.copyStream(HttpTest.class.getResourceAsStream(request.getRequestURI()), response.getOutputStream());
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
        connector.setKeystore(HttpTest.class.getResource("/nonguestablepassword.keystore").getFile()); // path to your keystroke file (depend what you've done in step 4(or 3 )
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
     * Test HTTP resources.
     *
     * @throws java.io.IOException if test fails
     */
    @Test
    public void httpResourceTest() throws IOException {
        Long startTime = System.currentTimeMillis();
        final WuicFacade facade = WuicFacade.newInstance("", "/wuic-http.xml");
        Long loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        startTime = System.currentTimeMillis();
        final List<WuicResource> group = facade.getGroup("css-image");
        loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        Assert.assertFalse(group.isEmpty());
        InputStream is;

        for (WuicResource res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
        }
    }
}
