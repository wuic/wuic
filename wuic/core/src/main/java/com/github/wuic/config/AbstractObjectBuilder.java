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


package com.github.wuic.config;

import com.github.wuic.exception.WuicException;
import com.github.wuic.util.PropertyResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Abstract implementation of what is a {@link ObjectBuilder}. It is composed of a set of {@link PropertySetter} used
 * to configure it before to call the {@link ObjectBuilder#build()} method.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 * @param <T> the type of built objects
 */
public abstract class AbstractObjectBuilder<T> extends PropertySetterRepository implements ObjectBuilder<T> {

    /**
     * Disallowed settings.
     */
    private Map<String, Object> disabled;

    /**
     * Managed by {@link PropertySetter}.
     */
    private Map<String, Object> properties;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     */
    public AbstractObjectBuilder() {
        properties = new HashMap<String, Object>();
        disabled = new HashMap<String, Object>();
    }

    /**
     * <p>
     * Gets the property {@code Map}.
     * </p>
     *
     * @return the properties
     */
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final PropertyResolver resolver) {
        for (final String key : getPropertyKeys()) {
            final String value = resolver.resolveProperty(key);

            if (value != null) {
                property(key, value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectBuilder<T> property(final String key, final Object value) {

        // Check if disabled
        if (disabled.containsKey(key)) {
            Object val = disabled.get(key);

            if ((val == null && value == null) || (val != null && val.equals(value))) {
                throwPropertyNotSupportedException(key + " (disabled)");
            }
        }

        boolean set = false;

        // Setting the value to the right property setter
        for (final PropertySetter[] setters : getPropertySetters()) {
            for (final PropertySetter setter : setters) {
                set |= setter.setProperty(key, value);
            }
        }

        // If no setter supports the property, throw an exception
        if (!set) {
            throwPropertyNotSupportedException(key);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectBuilder<T> disableSupport(final String key, final Object value) {
        disabled.put(key, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object property(final String key) {
        for (final PropertySetter[] setters : getPropertySetters()) {
            for (final PropertySetter setter : setters) {
                if (setter.getPropertyKey().equals(key)) {
                    return setter.get();
                }
            }
        }

        throwPropertyNotSupportedException(key);
        return this;
    }

    /**
     * <p>
     * Throws a particular {@link IllegalArgumentException} because a not supported property has been tried
     * to be set.
     * </p>
     *
     * @param key the property key
     */
    protected final void throwPropertyNotSupportedException(final String key) {
        WuicException.throwBuilderPropertyNotSupportedException(key, getClass());
    }
}
