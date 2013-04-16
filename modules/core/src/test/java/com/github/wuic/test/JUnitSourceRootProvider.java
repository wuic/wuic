////////////////////////////////////////////////////////////////////
//
// File: JUnitSourceRootProvider.java
// Created: 18 July 2012 10:00:00
// Author: GDROUET
// Copyright C 2012 Capgemini.
//
// All rights reserved.
//
////////////////////////////////////////////////////////////////////


package com.github.wuic.test;

import com.github.wuic.FileType;
import com.github.wuic.resource.impl.FileWuicResource;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.configuration.SourceRootProvider;

import java.io.File;
import java.io.IOException;

/**
 * <p>
 * {@link SourceRootProvider} for unit tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.1.0
 */
public class JUnitSourceRootProvider implements SourceRootProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResource getStreamResource(final String groupId, final String file)
            throws IOException {
        FileType ft = null;
        
        if (groupId.endsWith("js")) {
            ft = FileType.JAVASCRIPT;
        } else if (groupId.endsWith("css")) {
            ft = FileType.CSS;
        } else if (groupId.endsWith("image")) {
            ft = FileType.PNG;
        }

        return new FileWuicResource(new File("src/test/resources").getAbsolutePath(), file, ft);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean hasChanged(final String groupId, final String file, final Long since) {
        return Boolean.TRUE;
    }
}
