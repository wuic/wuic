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


package com.github.wuic.resource.impl;

import com.github.wuic.FileType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * <p>
 * Represents an in-memory resource.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.4
 * @since 0.2.0
 */
public class ByteArrayWuicResource extends AbstractWuicResource {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7883435600781576457L;

    /**
     * The bytes.
     */
    private byte[] byteArray;
    
    /**
     * <p>
     * Builds a new {@code WuicResource} based on a given byte array.
     * </p>
     * 
     * @param bytes the byte array
     */
    public ByteArrayWuicResource(final byte[] bytes, final String name, final FileType ft) {
        super(name, ft, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
        byteArray = Arrays.copyOf(bytes, bytes.length);
    }
    
    /**
     * <p>
     * Opens and returns an {@code InputStream} pointing to the resource. 
     * </p>
     * 
     * @return the opened input stream
     * @throws IOException if an I/O error occurs
     */
    public InputStream openStream() throws IOException {
        if (getName() == null) {
            return null;
        }
        
        return new ByteArrayInputStream(byteArray);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseDirectory() {
        return null;
    }
}
