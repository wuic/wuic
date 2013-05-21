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

import com.github.wuic.resource.WuicResource;
import com.github.wuic.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <p>
 * Base class to be extended by test tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.3.1
 */
public class WuicTest {

    /**
     * <p>
     * Writes on the disk the given resource if the system property 'wuic.test.storeTo' is set.
     * Useful to check if generated files are correct.
     * </p>
     *
     * @param name the file name on the disk
     * @param resource the resource
     * @throws java.io.IOException if an I/O error occurs
     */
    protected void writeToDisk(final WuicResource resource, final String name) throws IOException {
        final String dir = System.getProperty("wuic.test.storeTo");

        if (dir != null) {
            IOUtils.copyStream(resource.openStream(), new FileOutputStream(new File(dir, name)));
        }
    }
}
