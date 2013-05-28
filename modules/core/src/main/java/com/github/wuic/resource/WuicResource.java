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


package com.github.wuic.resource;

import com.github.wuic.FileType;
import com.github.wuic.util.InputStreamOpener;

import java.io.Serializable;

/**
 * <p>
 * Represents a resource provided or to be managed by the WUIC framework.
 * </p>
 *
 * TODO : refactor Configuration and WuicResource interfaces to manage from Configuration the cache, aggregation and compressions boolean.
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.1.1
 */
public interface WuicResource extends Serializable, InputStreamOpener {

    /**
     * <p>
     * Returns the file type of this resource.
     * </p>
     * 
     * @return the file type
     */
    FileType getFileType();
    
    /**
     * <p>
     * Returns the name of the resource. The name is a location relative to
     * the root directory returned by {@link WuicResource#getBaseDirectory()}.
     * </p>
     * 
     * @return the resource name
     */
    String getName();
    
    /**
     * <p>
     * Gets the root directory that contains all the files.
     * </p>
     * 
     * @return the root directory
     */
    String getBaseDirectory();

    /**
     * <p>
     * Indicates if this resource could be binary compressible.
     * </p>
     *
     * @return {@code true} if compressible, {@code false} otherwise
     */
    Boolean isBinaryCompressible();

    /**
     * <p>
     * Indicates if this resource is text compressible.
     * </p>
     *
     * @return {@code true} if compressible, {@code false} otherwise
     */
    Boolean isTextCompressible();

    /**
     * <p>
     * Indicates if this resource is cacheable.
     * </p>
     *
     * @return {@code true} if cacheable, {@code false} otherwise
     */
    Boolean isCacheable();

    /**
     * <p>
     * Indicates if the resource is aggregatable with other resources.
     * </p>
     *
     * @return {@code true} if aggregatable, {@code false} otherwise
     */
    Boolean isAggregatable();

    /**
     * <p>
     * Sets if this resource is binary compressible.
     * </p>
     *
     * @param tc {@code true} if compressible, {@code false} otherwise
     */
    void setBinaryCompressible(Boolean tc);

    /**
     * <p>
     * Sets if this resource is text compressible.
     * </p>
     *
     * @param tc {@code true} if compressible, {@code false} otherwise
     */
    void setTextCompressible(Boolean tc);

    /**
     * <p>
     * Sets if this resource is cacheable.
     * </p>
     *
     * @param c {@code true} if cacheable, {@code false} otherwise
     */
    void setCacheable(Boolean c);

    /**
     * <p>
     * Sets if the resource is aggregatable with other resources.
     * </p>
     *
     * @param a {@code true} if aggregatable, {@code false} otherwise
     */
    void setAggregatable(Boolean a);
}
