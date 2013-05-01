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

import com.github.wuic.Util;
import com.github.wuic.WuicFacade;
import com.github.wuic.resource.WuicResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
 * @version 1.1
 * @since 0.1.0
 */
@RunWith(JUnit4.class)
public class CoreTest {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    /**
     * Base test.
     * 
     * @throws IOException in I/O error case
     */
    @Test
    public void generationTest() throws IOException {
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
            
            is = res.openStream();
            FileUtils.copyInputStreamToFile(is, new File("C:\\" + i++ + "test.js"));
            is.close();
        }
        
        group = facade.getGroup("css");

        for (WuicResource res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readLines(is).size() > 0);
            is.close();
        }
        
        group = facade.getGroup("image");
        
        final Iterator<WuicResource> it = group.iterator();
        
        while (it.hasNext()) {
            InputStream fis = null;
            
            try {
                fis = it.next().openStream();
                final File file = new File("C:\\" + i++ + "test.js");
                FileUtils.copyInputStreamToFile(fis, file);
                final String content = FileUtils.readFileToString(file);
                final int start = content.indexOf("url\":\"") + 6;
                final int end = content.indexOf("/?file=aggregation.png");
                final String imageGroup = content.substring(start, end);
                group = facade.getGroup(imageGroup);

                FileUtils.copyInputStreamToFile(group.get(0).openStream(), new File("C:\\aggregate.png"));
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
        final Map<String, String> map = Util.orderedKeyMap();
        
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
}
