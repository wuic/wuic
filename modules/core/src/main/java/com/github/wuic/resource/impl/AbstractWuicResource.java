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
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.resource.WuicResource;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Base implementation of the {@link WuicResource} interface. A WuicResource is often represented by a name and a
 * {@link FileType}. This class already manages it.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.3.0
 */
public abstract class AbstractWuicResource implements WuicResource {

    /**
     * The path type.
     */
    private FileType fileType;

    /**
     * The path name.
     */
    private String fileName;

    /**
     * Text compressible or not.
     */
    private Boolean textCompressible;

    /**
     * Binary compressible or not.
     */
    private Boolean binaryCompressible;

    /**
     * Cacheable or not.
     */
    private Boolean cacheable;

    /**
     * Aggregatable or not.
     */
    private Boolean aggregatable;

    /**
     * Returns all the referenced resources.
     */
    private List<WuicResource> referencedResources;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param name the resource's name
     * @param ft the resource's type
     * @param bc binary compressible or not
     * @param tc text compressible or not
     * @param c cacheable or not
     * @param a aggregatable or not
     */
    protected AbstractWuicResource(final String name, final FileType ft, final Boolean bc, final Boolean tc, final Boolean c, final Boolean a) {
        if (ft == null) {
            throw new BadArgumentException(new IllegalArgumentException("You can't create a resource with a null FileType"));
        }

        if (name == null) {
            throw new BadArgumentException(new IllegalArgumentException("You can't create a resource with a null name"));
        }

        fileType = ft;
        fileName = name;
        binaryCompressible = bc;
        textCompressible = tc;
        cacheable = c;
        aggregatable = a;
        referencedResources = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileType getFileType() {
        return fileType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return fileName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isBinaryCompressible() {
        return binaryCompressible;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isTextCompressible() {
        return textCompressible;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isCacheable() {
        return cacheable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isAggregatable() {
        return aggregatable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBinaryCompressible(final Boolean bc) {
        binaryCompressible = bc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTextCompressible(final Boolean tc) {
        textCompressible = tc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCacheable(final Boolean c) {
        cacheable = c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAggregatable(final Boolean a) {
        aggregatable = a;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addReferencedResource(final WuicResource referenced) {
        if (referencedResources == null) {
            referencedResources = new ArrayList<WuicResource>();
        }

        // Do not allow duplicate resources (many resources with same name)
        if (referencedResources.contains(referenced)) {
            referencedResources.remove(referenced);
        }

        referencedResources.add(referenced);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> getReferencedResources() {
        return referencedResources;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return getClass().getSimpleName() + "[" + fileName + "]";
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object other) {
        if (other instanceof AbstractWuicResource) {
            return ((AbstractWuicResource) other).fileName.equals(fileName);
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return fileName.hashCode();
    }
}
