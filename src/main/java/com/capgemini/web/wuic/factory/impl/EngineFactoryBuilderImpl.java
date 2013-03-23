/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * •   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.capgemini.web.wuic.factory.impl;

import com.capgemini.web.wuic.FileType;
import com.capgemini.web.wuic.configuration.BadConfigurationException;
import com.capgemini.web.wuic.configuration.Configuration;
import com.capgemini.web.wuic.factory.EngineFactory;
import com.capgemini.web.wuic.factory.EngineFactoryBuilder;
import com.capgemini.web.wuic.xml.WuicXmlLoader;

import java.io.IOException;

/**
 * <p>
 * This builders use the {@link WuicXmlLoader} to launch configurations and creates
 * an {@link EngineFactory} according to this.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.1.0
 */
public class EngineFactoryBuilderImpl implements EngineFactoryBuilder {

    /**
     * The XML loader.
     */
    private WuicXmlLoader wuicXml;
    
    /**
     * <p>
     * Builds a new {@link EngineFactoryBuilderImpl}.
     * </p>
     * 
     * @throws IOException if an I/O error occurs if the 'wuic.xml' could not be loaded
     * @throws BadConfigurationException in the 'wuic.xml' is not well formed
     */
    public EngineFactoryBuilderImpl() throws IOException, BadConfigurationException {
        wuicXml = new WuicXmlLoader();
    }

    /**
     * {@inheritDoc}
     */
    public EngineFactory build() {
        final EngineFactoryImpl retval = new EngineFactoryImpl();
        
        // YUI support for CSS compression
        final Configuration cssConf = wuicXml.getConfiguration("yuicompressor-css");
        retval.addConfigurationForFileType(FileType.CSS, cssConf);

        // YUI support for JS compression
        final Configuration jsConf = wuicXml.getConfiguration("yuicompressor-js");
        retval.addConfigurationForFileType(FileType.JAVASCRIPT, jsConf);

        // Default compression for sprite files
        final Configuration spriteConf = wuicXml.getConfiguration("sprite-image-png");
        retval.addConfigurationForFileType(FileType.SPRITE, spriteConf);
        
        // Default compression for PNG files
        final Configuration pngConf = wuicXml.getConfiguration("image-png");
        retval.addConfigurationForFileType(FileType.PNG, pngConf);
        
        return retval;
    }
    
    /**
     * {@inheritDoc}
     */
    public WuicXmlLoader getLoader() {
        return wuicXml;
    }
}
