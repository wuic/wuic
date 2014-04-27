/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.util.IOUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Base implementation of the {@link Nut} interface. A Nut is often represented by a name and a
 * {@link com.github.wuic.NutType}. This class already manages it.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.6
 * @since 0.3.0
 */
public abstract class AbstractNut implements Nut {

    /**
     * The path type.
     */
    private NutType nutType;

    /**
     * The path name.
     */
    private String nutName;

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
     * Returns all the referenced nuts.
     */
    private List<Nut> referencedNuts;

    /**
     * The proxy URI.
     */
    private String proxyUri;

    /**
     * The original nuts.
     */
    private List<Nut> originalNuts;

    /**
     * The nut's version number.
     */
    private BigInteger versionNumber;

    /**
     * <p>
     * Creates a new instance by copying the given {@link Nut}.
     * </p>
     *
     * @param o the nut to copy
     */
    protected AbstractNut(final Nut o) {
        this(o.getName(),o.getNutType(), o.isBinaryCompressible(), o.isTextCompressible(), o.isCacheable(), o.isAggregatable(), o.getVersionNumber());
        referencedNuts = o.getReferencedNuts();
        originalNuts = o.getOriginalNuts();
    }

    /**
     * <p>
     * Creates a new instance. The nut is actually an original nut because constructor provides a version number of this
     * nut without any original nuts.
     * </p>
     *
     * @param name the nut's name
     * @param ft the nut's type
     * @param bc binary compressible or not
     * @param tc text compressible or not
     * @param c cacheable or not
     * @param a aggregatable or not
     * @param o the original nuts
     */
    protected AbstractNut(final String name,
                          final NutType ft,
                          final Boolean bc,
                          final Boolean tc,
                          final Boolean c,
                          final Boolean a,
                          final List<Nut> o) {
        this(name, ft, bc, tc, c, a, BigInteger.ZERO);
        originalNuts = o;

        // Computes a hash based on each original hash
        final MessageDigest md = IOUtils.newMessageDigest();

        for (final Nut original : originalNuts) {
            md.update(original.getVersionNumber().toString().getBytes());
        }

        versionNumber = new BigInteger(md.digest());
    }

    /**
     * <p>
     * Creates a new instance. The nut is actually an original nut because constructor provides a version number of this
     * nut without any original nuts.
     * </p>
     *
     * @param name the nut's name
     * @param ft the nut's type
     * @param bc binary compressible or not
     * @param tc text compressible or not
     * @param c cacheable or not
     * @param a aggregatable or not
     * @param v version number
     */
    protected AbstractNut(final String name,
                          final NutType ft,
                          final Boolean bc,
                          final Boolean tc,
                          final Boolean c,
                          final Boolean a,
                          final BigInteger v) {
        if (ft == null) {
            throw new BadArgumentException(new IllegalArgumentException("You can't create a nut with a null NutType"));
        }

        if (name == null) {
            throw new BadArgumentException(new IllegalArgumentException("You can't create a nut with a null name"));
        }

        nutType = ft;
        nutName = name;
        binaryCompressible = bc;
        textCompressible = tc;
        cacheable = c;
        aggregatable = a;
        referencedNuts = null;
        originalNuts = null;
        versionNumber = v;
    }

    /**
     * <p>
     * Sets the nut name.
     * </p>
     *
     * @param nutName the name
     */
    public final void setNutName(final String nutName) {
        this.nutName = nutName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutType getNutType() {
        return nutType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return nutName;
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
    public void addReferencedNut(final Nut referenced) {
        if (referencedNuts == null) {
            referencedNuts = new ArrayList<Nut>();
        }

        // Do not allow duplicate nuts (many nuts with same name)
        if (referencedNuts.contains(referenced)) {
            referencedNuts.remove(referenced);
        }
        referencedNuts.add(referenced);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> getReferencedNuts() {
        return referencedNuts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProxyUri() {
        return proxyUri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProxyUri(final String uri) {
        this.proxyUri = uri;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> getOriginalNuts() {
        return originalNuts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger getVersionNumber() {
        return versionNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getName() + "] - v" + getVersionNumber();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof AbstractNut) {
            return ((AbstractNut) other).nutName.equals(nutName);
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return nutName.hashCode();
    }
}
