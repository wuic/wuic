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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import com.github.wuic.util.IOUtils;
import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Core tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.5
 * @since 0.1.0
 */
@RunWith(JUnit4.class)
public class CoreTest extends WuicTest {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Test javascript compression.
     *
     * @throws Exception if test fails
     */
    @Test
    public void javascriptTest() throws Exception {
        Long startTime = System.currentTimeMillis();
        final WuicFacade facade = WuicFacade.newInstance("");
        Long loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        startTime = System.currentTimeMillis();
        List<WuicResource> group = facade.getGroup("util-js");
        loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        Assert.assertFalse(group.isEmpty());
        InputStream is;

        for (WuicResource res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
        }

        startTime = System.currentTimeMillis();
        group = facade.getGroup("util-js");
        loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));

        Assert.assertFalse(group.isEmpty());
        int i = 0;

        for (WuicResource res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
            writeToDisk(res, i++ + "test.js");
        }
    }

    /**
     * CSS compression test.
     * 
     * @throws Exception in I/O error case
     */
    @Test
    public void cssTest() throws Exception {
        // TODO : WUIC currently supports only one configuration per FileType. To be fixed in the future !
        Long startTime = System.currentTimeMillis();
        final WuicFacade facade = WuicFacade.newInstance("", "/wuic-css.xml");
        Long loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));
        InputStream is;
        List<WuicResource> group = facade.getGroup("css-image");
        int i = 0;

        for (WuicResource res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
            writeToDisk(res, i++ + "sprite.css");
        }

        group = facade.getGroup("css-scripts");
        i = 0;

        for (WuicResource res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
            writeToDisk(res, i++ + "css-script.css");
        }
    }

    /**
     * Javascript sprite test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void jsSpriteTest() throws Exception {
        Long startTime = System.currentTimeMillis();
        final WuicFacade facade = WuicFacade.newInstance("");
        Long loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));
        List<WuicResource> group = facade.getGroup("js-image");

        final Iterator<WuicResource> it = group.iterator();
        int i = 0;

        while (it.hasNext()) {
            InputStream fis = null;

            try {
                final String name = i++ + "sprite";
                WuicResource next = it.next();
                writeToDisk(next, name + ".js");

                fis = next.openStream();
                final File file = File.createTempFile(name, ".js");
                IOUtils.copyStream(fis, new FileOutputStream(file));
                final String content = IOUtils.readString(new InputStreamReader(new FileInputStream(file)));
                log.info(content);
                final int start = content.indexOf("url : \"") + 8;
                final int end = content.indexOf("/aggregate.png");
                final String imageGroup = content.substring(start, end);
                group = facade.getGroup(imageGroup);

                writeToDisk(group.get(0).getReferencedResources().get(0), "aggregate.png");
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
    }
}
