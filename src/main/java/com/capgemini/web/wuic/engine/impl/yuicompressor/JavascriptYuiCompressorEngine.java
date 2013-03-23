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


package com.capgemini.web.wuic.engine.impl.yuicompressor;

import com.capgemini.web.wuic.configuration.Configuration;
import com.capgemini.web.wuic.configuration.YuiJavascriptConfiguration;
import com.capgemini.web.wuic.engine.impl.embedded.CGAbstractCompressorEngine;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;

/**
 * <p>
 * This class can compress Javascript files using the YUI compressor library.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.1.0
 */
public class JavascriptYuiCompressorEngine extends CGAbstractCompressorEngine {
    
    /**
     * Marker that identifies zones where characters are escaped.
     */
    private static final String ESCAPE_MARKER = "WUIC_ESCAPE_BACKSLASH";
    
    /**
     * Special characters (to prefix with a backslash) to be escaped before compression.
     */
    private static final String[] TO_ESCAPE = {"n", "t", "r"};
    
    /**
     * The configuration.
     */
    private YuiJavascriptConfiguration configuration;
    
    /**
     * <p>
     * Creates a new {@link com.capgemini.web.wuic.engine.Engine}. An
     * {@link IllegalArgumentException} will be thrown if the configuration
     * is not a {@link JavascriptYuiCompressorEngine}.
     * </p>
     * 
     * @param config the {@link Configuration}
     */
    public JavascriptYuiCompressorEngine(final Configuration config) {
        if (config instanceof YuiJavascriptConfiguration) {
            configuration = (YuiJavascriptConfiguration) config;
        } else {
            final String message = config + " must be an instance of " + YuiJavascriptConfiguration.class.getName();
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void compress(final InputStream source, final OutputStream target)
            throws IOException {
        Reader in = null;
        StringWriter out = null;
        Writer targetOut = null;
        
        try {
            // Stream to read from the source
            in = new InputStreamReader(switchSpecialChars(source, Boolean.FALSE), configuration.charset());
            
            // Create the compressor using the source stream
            final JavaScriptCompressor compressor =
                    new JavaScriptCompressor(in, new JavascriptYuiCompressorErrorReporter());
            
            // Now close the stream read
            in.close();
            in = null;
            
            // Write into a temporary buffer with escaped special characters
            out = new StringWriter();
            
            // Compress the script into the temporary buffer
            compressor.compress(out,
                    configuration.yuiLineBreakPos(),
                    configuration.yuiMunge(),
                    configuration.yuiVerbose(),
                    configuration.yuiPreserveAllSemiColons(),
                    configuration.yuiDisableOptimizations());
            
            // Stream to write into the target with backed special characters
            final InputStream bis = new ByteArrayInputStream(out.getBuffer().toString().getBytes());
            final InputStream restore = switchSpecialChars(bis, Boolean.TRUE);
            targetOut = new OutputStreamWriter(target);
            IOUtils.copy(restore, targetOut, configuration.charset());
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(targetOut);
        }
    }

    /**
     * <p>
     * This methods escapes or restore the \n, \r and \t characters of the given
     * stream and returns the result of the operation. Fixes escape issue with
     * YUICompressor. 
     * </p>
     * 
     * @param source the source to escape
     * @param restore flag that indicates if we escape or restore
     * @return the result
     * @throws IOException if an I/O error occurs
     */
    private InputStream switchSpecialChars(final InputStream source, final Boolean restore) throws IOException {
        final Reader parser = new InputStreamReader(source, configuration.charset());
        final File tempFile = File.createTempFile(String.valueOf(System.nanoTime()), ".wuic");
        OutputStream streamParser = null;
        
        try {
            streamParser = new FileOutputStream(tempFile);
            final char[] buffer = new char[2048];
            int offset;
            
            while ((offset = parser.read(buffer)) != -1) {
                String read = new String(buffer, 0, offset);
                
                for (String c : TO_ESCAPE) {
                    if (restore) {
                        read = read.replace(ESCAPE_MARKER + c, "\\" + c);
                    } else {
                        read = read.replace("\\" + c, ESCAPE_MARKER + c);
                    }
                }
                
                streamParser.write(read.getBytes());
            }
        } finally {
            IOUtils.closeQuietly(streamParser);
        }
        
        return new FileInputStream(tempFile);
    }

    /**
     * {@inheritDoc}
     */
    public Configuration getConfiguration() {
        return configuration;
    }
}
