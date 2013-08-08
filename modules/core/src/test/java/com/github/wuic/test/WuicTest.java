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

import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;

/**
 * <p>
 * Base class to be extended by test tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.1
 */
public class WuicTest {

    /**
     * <p>
     * Writes on the disk the given nut if the system property 'wuic.test.storeTo' is set.
     * Useful to check if generated files are correct.
     * </p>
     *
     * @param name the path name on the disk
     * @param resource the nut
     * @throws Exception if an I/O error occurs
     */
    protected void writeToDisk(final Nut resource, final String name) throws Exception {
        final String dir = System.getProperty("wuic.test.storeTo");

        if (dir != null) {
            IOUtils.copyStream(resource.openStream(), new FileOutputStream(new File(dir, name)));
        }
    }
}
