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


package com.github.wuic.nut.builder;

import com.github.wuic.exception.WuicRdbPropertyNotSupportedException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.NutDaoBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Abstract implementation of what is a {@link com.github.wuic.nut.NutDaoBuilder}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.1
 */
public abstract class AbstractNutDaoBuilder implements NutDaoBuilder {

    /**
     * Package access level for {@link PropertySetter}.
     */
    private Map<String, Object> properties;

    /**
     * The property setters define which property is supported.
     */
    private PropertySetter[] propertySetters;

    /**
     * <p>
     * Builds a new {@link com.github.wuic.nut.NutDaoBuilder} with specific property setters.
     * </p>
     *
     * @param setters the specific {@link PropertySetter setters}
     */
    protected AbstractNutDaoBuilder(final PropertySetter... setters) {
        properties = new HashMap<String, Object>();
        propertySetters = setters;
    }

    /**
     * <p>
     * Gets the property {@code Map}.
     * </p>
     *
     * @return the properties
     */
    protected Map<String, Object> getProperties() {
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
    public NutDaoBuilder property(final String key, final Object value) throws WuicRdbPropertyNotSupportedException {

        for (PropertySetter setter : propertySetters) {
            if (setter.setProperty(key, value)) {
                return this;
            }
        }

        throw new WuicRdbPropertyNotSupportedException(key, this.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object property(final String key) throws WuicRdbPropertyNotSupportedException {

        for (PropertySetter setter : propertySetters) {
            if (setter.getPropertyKey().equals(key)) {
                return setter.get();
            }
        }

        throw new WuicRdbPropertyNotSupportedException(key, this.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutDao build() {
        try {
            return internalBuild();
        } catch (WuicRdbPropertyNotSupportedException wre) {
            throw new BadArgumentException(new IllegalArgumentException(wre));
        }
    }

    /**
     * <p>
     * Builds internally a {@link NutDao} as specified by {@link com.github.wuic.nut.NutDaoBuilder#build()} without
     * catching any exception.
     * </p>
     *
     * @return the {@link NutDao}
     * @throws WuicRdbPropertyNotSupportedException the uncaught exception
     */
    protected abstract NutDao internalBuild() throws WuicRdbPropertyNotSupportedException;
}
