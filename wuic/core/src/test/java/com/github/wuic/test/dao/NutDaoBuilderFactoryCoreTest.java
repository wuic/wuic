/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.test.dao;

import com.github.wuic.config.ObjectBuilderFactory;

import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoService;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * <p>
 * {@link NutDao} builder factory support for core module tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class NutDaoBuilderFactoryCoreTest {

    /**
     * The factory.
     */
    private static ObjectBuilderFactory<NutDao> nutDaoObjectBuilderFactory;

    /**
     * Initializes the factory.
     */
    @BeforeClass
    public static void initFactory() {
        nutDaoObjectBuilderFactory = new ObjectBuilderFactory<NutDao>(NutDaoService.class, NutDaoService.DEFAULT_SCAN_PACKAGE);
    }

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Test for unknown builder.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateUnknownBuilder() {
        nutDaoObjectBuilderFactory.create("FooNutDaoBuilder");
    }

    /**
     * Test for classpath.
     */
    @Test
    public void testCreateClasspathBuilder() {
        Assert.assertNotNull(nutDaoObjectBuilderFactory.create("ClasspathNutDaoBuilder"));
    }

    /**
     * Test for HTTP.
     */
    @Test
    public void testCreateHttpBuilder() {
        Assert.assertNotNull(nutDaoObjectBuilderFactory.create("HttpNutDaoBuilder"));
    }

    /**
     * Test for disk.
     */
    @Test
    public void testCreateDiskBuilder() {
        Assert.assertNotNull(nutDaoObjectBuilderFactory.create("DiskNutDaoBuilder"));
    }
}
