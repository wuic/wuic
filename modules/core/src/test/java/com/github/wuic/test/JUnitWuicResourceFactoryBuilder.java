////////////////////////////////////////////////////////////////////
//
// File: JUnitWuicResourceFactoryBuilder.java
// Created: 18 July 2012 10:00:00
// Author: GDROUET
// Copyright C 2012 Capgemini.
//
// All rights reserved.
//
////////////////////////////////////////////////////////////////////


package com.github.wuic.test;

import com.github.wuic.FileType;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.resource.WuicResourceFactory;
import com.github.wuic.resource.WuicResourceFactoryBuilder;
import com.github.wuic.resource.impl.disk.FileWuicResource;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.util.CollectionUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * {@link WuicResourceFactory} for unit tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.1.0
 */
public class JUnitWuicResourceFactoryBuilder implements WuicResourceFactoryBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResourceFactoryBuilder regex() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResourceFactoryBuilder property(String key, String value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResourceFactory build() {
        return new WuicResourceFactory() {
            @Override
            public List<WuicResource> create(String path) throws StreamException {

                FileType ft = null;

                if (path.endsWith("js")) {
                    ft = FileType.JAVASCRIPT;
                } else if (path.endsWith("css")) {
                    ft = FileType.CSS;
                } else if (path.endsWith("image")) {
                    ft = FileType.PNG;
                }

                final String prefix = System.getProperty("wuic.test.rootDirectoryPrefix");
                final String resources = prefix == null ? "src/test/resources" : prefix + "/src/test/resources";
                final WuicResource res = new FileWuicResource(new File(resources).getAbsolutePath(), path, ft);

                return CollectionUtils.newList(res);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public List<String> computeRealPaths(final String path) throws StreamException {
                return Arrays.asList(path);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void setProperty(final String key, final String value) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
