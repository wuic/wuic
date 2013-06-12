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
 * -   The above copyright notice and this permission notice shall be included in
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


package com.github.wuic.configuration.impl;

import com.github.wuic.exception.UnableToInstantiateException;
import com.github.wuic.exception.xml.WuicXmlException;
import com.github.wuic.configuration.Configuration;
import com.github.wuic.configuration.DomConfigurationBuilder;

import com.github.wuic.exception.xml.WuicXmlMissingConfigurationElementException;
import com.github.wuic.exception.xml.WuicXmlNoConfigurationIdAttributeException;
import com.github.wuic.exception.xml.WuicXmlUnableToInstantiateException;
import net.sf.ehcache.Cache;

import org.w3c.dom.Node;

/**
 * <p>
 * This builder produces {@link com.github.wuic.configuration.SpriteConfiguration} instances.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.1.0
 */
public class SpriteConfiguratonDomBuilder implements DomConfigurationBuilder {

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration build(final Node nodeConfiguration, final Cache cache)
            throws WuicXmlException {
        // Expected elements
        String doCache = null;
        String compress = null;
        String aggregate = null;
        String spriteClassName = null;
        String charset = null;
        
        // Find in the children of the given node the expected elements
        for (int i = 0; i < nodeConfiguration.getChildNodes().getLength(); i++) {
            final Node child = nodeConfiguration.getChildNodes().item(i);

            if ("cache".equals(child.getNodeName())) {
                doCache = child.getFirstChild().getTextContent();
            } else if ("compress".equals(child.getNodeName())) {
                compress = child.getFirstChild().getTextContent();
            } else if ("aggregate".equals(child.getNodeName())) {
                aggregate = child.getFirstChild().getTextContent();
            } else if ("charset".equals(child.getNodeName())) {
                charset = child.getFirstChild().getTextContent();
            } else if ("sprite-provider".equals(child.getNodeName())) {
                spriteClassName = child.getFirstChild().getTextContent();
            }
        }
        
        // If one element is null, then throw an exception
        if (compress == null || aggregate == null || spriteClassName == null || charset == null) {
            throw new WuicXmlMissingConfigurationElementException("compress", "aggregate", "charset", "sprite-provider");
        } 

        // Check if the ID exists to get it
        final Node idAttribute = nodeConfiguration.getAttributes().getNamedItem("id");
        
        if (idAttribute == null) {
            throw new WuicXmlNoConfigurationIdAttributeException();
        }
        
        final String id = idAttribute.getNodeValue();

        try {
            // Create and return the configuration
            return new SpriteConfigurationImpl(id,
                    Boolean.parseBoolean(doCache),
                    Boolean.parseBoolean(compress),
                    Boolean.parseBoolean(aggregate),
                    charset,
                    cache,
                    spriteClassName);
        } catch (UnableToInstantiateException utie) {
            throw new WuicXmlUnableToInstantiateException(utie);
        }
    }
}
