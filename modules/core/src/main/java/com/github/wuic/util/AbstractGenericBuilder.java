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


package com.github.wuic.util;

import com.github.wuic.exception.BuilderPropertyNotSupportedException;
import com.github.wuic.exception.wrapper.BadArgumentException;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Abstract implementation of what is a {@link GenericBuilder}. It is composed of a set of {@link PropertySetter} used
 * to configure it before to call the {@link com.github.wuic.util.GenericBuilder#build()} method.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 * @param <T> the type of built objects
 */
public abstract class AbstractGenericBuilder<T> implements GenericBuilder<T> {

    /**
     * Managed by {@link PropertySetter}.
     */
    private Map<String, Object> properties;

    /**
     * The property setters define which property is supported.
     */
    private PropertySetter[] propertySetters;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param setters the setters
     */
    public AbstractGenericBuilder(final PropertySetter ... setters) {
        propertySetters = setters;
        properties = new HashMap<String, Object>();
    }

    /**
     * <p>
     * Gets the property {@code Map}.
     * </p>
     *
     * @return the properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * <p>
     * Adds the given {@link PropertySetter setters}.
     * </p>
     *
     * @param setters the setters
     */
    protected void addPropertySetter(final PropertySetter ... setters) {
        // Merges the two arrays. Each array should not be null.
        final PropertySetter[] target = new PropertySetter[setters.length + propertySetters.length];
        System.arraycopy(propertySetters, 0, target, 0, propertySetters.length);
        System.arraycopy(setters, 0, target, propertySetters.length, setters.length);
        propertySetters = target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericBuilder property(final String key, final Object value) throws BuilderPropertyNotSupportedException {

        for (PropertySetter setter : propertySetters) {
            if (setter.setProperty(key, value)) {
                return this;
            }
        }

        throwPropertyNotSupportedException(key);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object property(final String key) throws BuilderPropertyNotSupportedException {

        for (PropertySetter setter : propertySetters) {
            if (setter.getPropertyKey().equals(key)) {
                return setter.get();
            }
        }

        throwPropertyNotSupportedException(key);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T build() {
        try {
            return internalBuild();
        } catch (BuilderPropertyNotSupportedException wre) {
            throw new BadArgumentException(new IllegalArgumentException(wre));
        }
    }

    /**
     * <p>
     * Builds internally an object as specified by {@link com.github.wuic.nut.NutDaoBuilder#build()} without
     * catching any exception.
     * </p>
     *
     * @return the object
     * @throws com.github.wuic.exception.BuilderPropertyNotSupportedException the uncaught exception
     */
    protected abstract T internalBuild() throws BuilderPropertyNotSupportedException;

    /**
     * <p>
     * Throws a particular {@link BuilderPropertyNotSupportedException} because a not supported property has been tried
     * to be set.
     * </p>
     *
     * @param key the property key
     * @throws BuilderPropertyNotSupportedException the exception
     */
    protected abstract void throwPropertyNotSupportedException(String key) throws BuilderPropertyNotSupportedException;
}
