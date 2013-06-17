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

import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>
 * Utils classes tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.4
 */
@RunWith(JUnit4.class)
public class UtilityTest extends WuicTest {

    /**
     * Test string merge.
     */
    @Test
    public void mergeTest() {
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", "oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo:", "oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo:", ":oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", ":oof", }, ":"), "foo:oof");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", ":oof", "foo" }, ":"), "foo:oof:foo");
        Assert.assertEquals(StringUtils.merge(new String[] {"foo", ":oof", "foo" }, null), "foo:ooffoo");
    }

    /**
     * Be sure that the {@code Map} used internally keep the order of the keys.
     */
    @Test
    public void orderingKeyMapTest() {
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
     * Makes sure the research is correctly performed.
     * 
     * Actually if you have this directory : /foo/oof/js/file.js
     * You have a classpath where root is /foo i.e file.js is retrieved thanks to /oof/js/file.js
     * Your classpath protocol has base directory /oof
     * We need to be very clear about the path evaluated by the regex
     * For instance, /.*.js should returns /js/file.js since /oof is the base path
     * After, that /oof + /js/file.js will result in the exact classpath entry to retrieve
     *
     * @throws IOException if any I/O error occurs
     * @throws StreamException if error occurs during research
     */
    @Test
    public void fileSearchTest() throws IOException, StreamException {
        final String nanoTime = String.valueOf(System.nanoTime());
        final File basePath = new File(StringUtils.merge(new String[]{ System.getProperty("java.io.tmpdir"), nanoTime, }, "/"));
        basePath.mkdirs();

        final File file = File.createTempFile("file", ".js", basePath);

        List<String> list = IOUtils.lookupFileResources(System.getProperty("java.io.tmpdir"), nanoTime, Pattern.compile("/.*js"));

        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0), nanoTime + "/" + file.getName());
    }
}
