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

import com.github.wuic.exception.UnableToInstantiateException;

import com.github.wuic.nut.builder.NutDaoBuilderFactory;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * <p>
 * {@link NutDaoBuilderFactory} support for nut module tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class NutDaoBuilderFactoryCoreTest {

    /**
     * Test for classpath.
     */
    @Test
    public void testCreateClasspathBuilder() throws UnableToInstantiateException {
        Assert.assertNotNull(NutDaoBuilderFactory.getInstance().create("ClasspathNutDaoBuilder"));
    }

    /**
     * Test for HTTP.
     */
    @Test
    public void testCreateHttpBuilder() throws UnableToInstantiateException {
        Assert.assertNotNull(NutDaoBuilderFactory.getInstance().create("HttpNutDaoBuilder"));
    }

    /**
     * Test for disk.
     */
    @Test
    public void testCreateDiskBuilder() throws UnableToInstantiateException {
        Assert.assertNotNull(NutDaoBuilderFactory.getInstance().create("DiskNutDaoBuilder"));
    }
}
