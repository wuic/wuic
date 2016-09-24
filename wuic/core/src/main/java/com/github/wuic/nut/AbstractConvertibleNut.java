/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.FutureWrapper;
import com.github.wuic.util.Input;
import com.github.wuic.util.Pipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * Base class for {@link ConvertibleNut} implementation with state management (attributes like transformers or referenced
 * nuts). Transformation is delegated to subclass.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public abstract class AbstractConvertibleNut extends AbstractNut implements ConvertibleNut {

    /**
     * The path name.
     */
    private String nutName;

    /**
     * The nut type.
     */
    private NutType nutType;

    /**
     * Returns all the referenced nuts.
     */
    private List<ConvertibleNut> referencedNuts;

    /**
     * The original nuts.
     */
    private Source source;

    /**
     * Some transformers.
     */
    private Set<Pipe.Transformer<ConvertibleNut>> transformers;

    /**
     * Callbacks.
     */
    private transient List<Pipe.OnReady> onReady;

    /**
     * Compressed or not.
     */
    private Boolean compressed;

    /**
     * Sub resource or not.
     */
    private boolean subResource;

    /**
     * Ignores the composite stream during transformation.
     */
    private boolean ignoreCompositeStreamOnTransformation;

    /**
     * The callbacks invoked to modify the version number.
     */
    private List<BiFunction<ConvertibleNut, Long, Long>> versionNumberCallbacks;

    /**
     * Converted nut.
     */
    private Nut wrap;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param o the converted nut
     */
    protected AbstractConvertibleNut(final Nut o) {
        super(o);
        wrap = o;

        if (ConvertibleNut.class.isAssignableFrom(o.getClass())) {
            final ConvertibleNut c = ConvertibleNut.class.cast(o);
            transformers = c.getTransformers() != null ? new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>(c.getTransformers()) : null;
            onReady = new ArrayList<Pipe.OnReady>(c.getReadyCallbacks());
            source = c.getSource();
            ignoreCompositeStreamOnTransformation = c.ignoreCompositeStreamOnTransformation();
            setNutName(c.getName());
            setNutType(c.getNutType());
            referencedNuts = c.getReferencedNuts();
            setIsCompressed(c.isCompressed());
            setIsSubResource(c.isSubResource());
            versionNumberCallbacks = getVersionNumberCallbacks();

            // Copy the list
            if (versionNumberCallbacks != null) {
                versionNumberCallbacks = new ArrayList<BiFunction<ConvertibleNut, Long, Long>>(versionNumberCallbacks);
            }
        } else {
            setNutName(o.getInitialName());
            setNutType(o.getInitialNutType());
            setIsCompressed(Boolean.FALSE);
            setIsSubResource(true);
            source = new SourceImpl();
            ignoreCompositeStreamOnTransformation = false;
        }
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param name the nut's name
     * @param ft the nut's type
     * @param v version number
     * @param c is compressed or not
     */
    protected AbstractConvertibleNut(final String name,
                                     final NutType ft,
                                     final Future<Long> v,
                                     final Boolean c) {
        super(name, ft, v);
        setNutName(name);
        setNutType(ft);
        setIsCompressed(c);
        setIsSubResource(true);
        source = new SourceImpl();
        ignoreCompositeStreamOnTransformation = false;
    }

    /**
     * <p>
     * Sets the original nuts for the given {@link Source object}.
     * </p>
     *
     * @param o the original nuts
     */
    protected final void setOriginalNuts(final Source src, final List<ConvertibleNut> o) {
        for (final ConvertibleNut c : o) {
            src.addOriginalNut(c);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Pipe.Transformer<ConvertibleNut>> getTransformers() {
        return transformers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Pipe.OnReady> getReadyCallbacks() {
        return onReady == null ? Collections.<Pipe.OnReady>emptyList() : Collections.unmodifiableList(new ArrayList<Pipe.OnReady>(onReady));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Boolean isCompressed() {
        return compressed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setIsCompressed(final Boolean compressed) {
        this.compressed = compressed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setNutName(final String nutName) {
        this.nutName = nutName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setNutType(final NutType nutType) {
        this.nutType = nutType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setIsSubResource(final boolean sr) {
        subResource = sr;
    }

    @Override
    public final boolean isSubResource() {
        return subResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addTransformer(final Pipe.Transformer<ConvertibleNut> transformer) {
        if (transformers == null) {
            transformers = new LinkedHashSet<Pipe.Transformer<ConvertibleNut>>();
        }

        transformers.add(transformer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReady(final Pipe.OnReady callback, final boolean removeAfterInvocation) {
        if (onReady == null) {
            onReady = new ArrayList<Pipe.OnReady>();
        }

        onReady.add(removeAfterInvocation ? new RemoveCallBackOnInvocation(callback) : callback);
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
    public NutType getNutType() {
        return nutType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addReferencedNut(final ConvertibleNut referenced) {
        if (referenced.isDynamic()) {
            WuicException.throwBadArgumentException(new IllegalArgumentException(
                    String.format("Nut '%s' is dynamic and could not be referenced by '%s'.", referenced.getName(), getName())));
        }

        if (referencedNuts == null) {
            referencedNuts = new ArrayList<ConvertibleNut>();
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
    public List<ConvertibleNut> getReferencedNuts() {
        return referencedNuts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Source getSource() {
        return source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSource(final Source src) {
        setOriginalNuts(src, source.getOriginalNuts());
        this.source = src;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        return wrap.openStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic() {
        return wrap.isDynamic();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParentFile() {
        return wrap.getParentFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ignoreCompositeStreamOnTransformation() {
        return ignoreCompositeStreamOnTransformation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BiFunction<ConvertibleNut, Long, Long>> getVersionNumberCallbacks() {
        return versionNumberCallbacks == null ? null : Collections.unmodifiableList(versionNumberCallbacks);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addVersionNumberCallback(final BiFunction<ConvertibleNut, Long, Long> callback) {
        if (versionNumberCallbacks == null) {
            versionNumberCallbacks = new ArrayList<BiFunction<ConvertibleNut, Long, Long>>();
        }

        versionNumberCallbacks.add(callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<Long> getVersionNumber() {
       return new VersionNumberAdapter(super.getVersionNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object other) {
        if (other instanceof ConvertibleNut) {
            return ((ConvertibleNut) other).getName().equals(getName());
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    /**
     * <p>
     * A {@code Future} that applies the callbacks {@link #versionNumberCallbacks} to alter the returned {@code Long}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private class VersionNumberAdapter extends FutureWrapper<Long> {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param delegate the delegated future
         */
        private VersionNumberAdapter(final Future<Long> delegate) {
            super(delegate);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Long get() throws InterruptedException, ExecutionException {
            return adapt(super.get());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Long get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return adapt(super.get(timeout, unit));
        }

        /**
         * <p>
         * Adapts the given version number by calling each {@code BiFunction} in the {@link #versionNumberCallbacks} list.
         * </p>
         *
         * @param versionNumber the version number
         * @return the modified version number if {@link #versionNumberCallbacks} is not {@code null}, the unchanged value otherwise
         */
        private Long adapt(final Long versionNumber) {
            Long retval = versionNumber;

            if (versionNumberCallbacks != null) {
                for (final BiFunction<ConvertibleNut, Long, Long> cb : versionNumberCallbacks) {
                    retval = cb.apply(AbstractConvertibleNut.this, retval);
                }
            }

            return retval < 0 ? retval * -1 : retval;
        }
    }

    /**
     * <p>
     * Removes the this callback from {@link #onReady} when invoked.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private class RemoveCallBackOnInvocation implements Pipe.OnReady {

        /**
         * Wrapped callback.
         */
        private final Pipe.OnReady wrapped;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param onReady the callback
         */
        private RemoveCallBackOnInvocation(final Pipe.OnReady onReady) {
            this.wrapped = onReady;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void ready(final Pipe.Execution e) throws IOException {
            wrapped.ready(e);
            onReady.remove(this);
        }
    }
}
