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

package com.github.wuic.engine.impl.yuicompressor;

import com.github.wuic.configuration.Configuration;
import com.github.wuic.configuration.YuiCssConfiguration;
import com.github.wuic.engine.impl.embedded.CGAbstractCompressorEngine;
import com.github.wuic.util.IOUtils;
import com.yahoo.platform.yui.compressor.CssCompressor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * <p>
 * This class can compress CSS files using the YUI compressor library.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.1.0
 */
public class CssYuiCompressorEngine extends CGAbstractCompressorEngine {

    /**
     * The configuration.
     */
    private YuiCssConfiguration configuration;
    
    /**
     * <p>
     * Creates a new {@link com.github.wuic.engine.Engine}. An
     * {@link IllegalArgumentException} will be thrown if the configuration
     * is not a {@link CssYuiCompressorEngine}.
     * </p>
     * 
     * @param config the {@link Configuration}
     */
    public CssYuiCompressorEngine(final Configuration config) {
        if (config instanceof YuiCssConfiguration) {
            configuration = (YuiCssConfiguration) config;
        } else {
            final String message = config + " must be an instance of " + YuiCssConfiguration.class.getName();
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void compress(final InputStream source, final OutputStream target)
            throws IOException {
        Reader in = null;
        Writer out = null;
        
        try {
            // Stream to read from the source
            in = new InputStreamReader(source, configuration.charset());
     
            // Create the compressor using the source stream
            final CssCompressor compressor = new CssCompressor(in);
            
            // Now close the stream read
            in.close();
            in = null;
            
            // Stream to write into the target
            out = new OutputStreamWriter(target, configuration.charset());
            
            // Compress the script into the output target
            compressor.compress(out, configuration.yuiLineBreakPos());
        } finally {
            IOUtils.close(in);
            IOUtils.close(out);
        }
    }
}
