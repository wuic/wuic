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

import com.github.wuic.NutType;
import com.github.wuic.configuration.YuiConfiguration;
import com.github.wuic.configuration.YuiJavascriptConfiguration;

/**
 * <p>
 * Implementation as a POJO of the {@link YuiJavascriptConfiguration}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.1.0
 */
public class YuiJavascriptConfigurationImpl extends YuiConfigurationImpl implements YuiJavascriptConfiguration {

    /**
     * YUI 'munge' option.
     */
    private Boolean yuiMunge;
    
    /**
     * YUI 'preserveAllSemiColons' option.
     */
    private Boolean yuiPreserveSemiColons;
    
    /**
     * YUI 'disableOptimizations' option.
     */
    private Boolean yuiDisableOptimizations;
    
    /**
     * YUI 'verbose' option.
     */
    private Boolean yuiVerbose;

    /**
     * <p>
     * Builds a new instance based on a given global {@link YuiConfiguration configuration}.
     * Additional settings specific to Javascript compression by YUI compressor are
     * also provided.
     * </p>
     * 
     * @param base the base {@link YuiConfiguration configuration}
     * @param munge 'munge' or not
     * @param verbose be verbose or not
     * @param preserveSemiColons preserve semicolons or not
     * @param disableOptimizations disable optimizations or not
     */
    public YuiJavascriptConfigurationImpl(final YuiConfiguration base,
            final Boolean munge,
            final Boolean verbose,
            final Boolean preserveSemiColons,
            final Boolean disableOptimizations) {
        super(base);
        
        yuiMunge = munge;
        yuiVerbose = verbose;
        yuiPreserveSemiColons = preserveSemiColons;
        yuiDisableOptimizations = disableOptimizations;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean yuiMunge() {
        return yuiMunge;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean yuiVerbose() {
        return yuiVerbose;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean yuiPreserveAllSemiColons() {
        return yuiPreserveSemiColons;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean yuiDisableOptimizations() {
        return yuiDisableOptimizations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutType getNutType() {
        return NutType.JAVASCRIPT;
    }
}
