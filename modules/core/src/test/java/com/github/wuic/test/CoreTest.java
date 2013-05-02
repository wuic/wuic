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
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.wuic.util.CollectionUtils;
import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
 * @version 1.2
 * @since 0.1.0
 */
@RunWith(JUnit4.class)
public class CoreTest {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Test javascript compression.
     *
     * @throws IOException if test fails
     */
    @Test
    public void javascriptTest() throws IOException {
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
            Assert.assertTrue(IOUtils.readLines(is).size() > 0);
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
            Assert.assertTrue(IOUtils.readLines(is).size() > 0);
            is.close();
            writeToDisk(res, i++ + "test.js");
        }
    }

    /**
     * CSS compression test.
     * 
     * @throws IOException in I/O error case
     */
    @Test
    public void cssTest() throws IOException {
        // TODO : WUIC currently supports only one configuration per FileType. To be fixed in the future !
        Long startTime = System.currentTimeMillis();
        final WuicFacade facade = WuicFacade.newInstance("", "/wuic-css.xml");
        Long loadTime = System.currentTimeMillis() - startTime;
        log.info(String.valueOf(((float) loadTime / 1000)));
        InputStream is;
        final List<WuicResource> group = facade.getGroup("css-image");
        int i = 0;

        for (WuicResource res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readLines(is).size() > 0);
            is.close();
            writeToDisk(res, i++ + "sprite.css");
        }
    }

    /**
     * Javascript sprite test.
     *
     * @throws IOException if test fails
     */
    @Test
    public void jsSpriteTest() throws IOException {
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
                FileUtils.copyInputStreamToFile(fis, file);
                final String content = FileUtils.readFileToString(file);
                final int start = content.indexOf("url\":\"") + 6;
                final int end = content.indexOf("/?file=aggregation.png");
                final String imageGroup = content.substring(start, end);
                group = facade.getGroup(imageGroup);

                writeToDisk(group.get(0), "aggregation.png");
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
    }

    /**
     * Be sure that the {@code Map} used internally keep the order of the keys.
     */
    @Test
    public void orderingKeyMap() {
        final Map<String, String> map = CollectionUtils.orderedKeyMap();
        
        map.put("toto", "");
        map.put("titi", "");
        map.put("tata", "");
        map.put("tutu", "");
        
        int cpt = 0;
        
        for (String key : map.keySet()) {
            Assert.assertTrue(cpt == 0 ? "toto".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 1 ? "titi".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 2 ? "tata".equals(key) : Boolean.TRUE);
            Assert.assertTrue(cpt == 3 ? "tutu".equals(key) : Boolean.TRUE);
            cpt++;
        }
    }

    /**
     * <p>
     * Writes on the disk the given resource if the system property 'wuic.test.storeTo' is set.
     * Useful to check if generated files are correct.
     * </p>
     *
     * @param name the file name on the disk
     * @param resource the resource
     * @throws IOException if an I/O error occurs
     */
    private void writeToDisk(final WuicResource resource, final String name) throws IOException {
        final String dir = System.getProperty("wuic.test.storeTo");

        if (dir != null) {
            FileUtils.copyInputStreamToFile(resource.openStream(), new File(dir, name));
        }
    }
}
