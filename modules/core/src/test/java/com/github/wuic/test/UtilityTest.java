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
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.StringUtils;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
}
