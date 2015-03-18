/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.config;

import java.util.Map;

/**
 * <p>
 * This interface describes the contract for a class which implements the builder pattern inside WUIC. Because objects
 * can be built in so many different ways, the builder has very abstract behavior. Its contains a {@link ObjectBuilder#property(String, Object)}
 * method to configure the state of the built objects.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 * @param <T> the built type
 */
public interface ObjectBuilder<T> {

    /**
     * <p>
     * Decorates this builder with a property.
     * </p>
     *
     * <p>
     * Throws an {@link IllegalArgumentException} if the property key is not supported or if the value is incorrect.
     * </p>
     *
     * @param key the key
     * @param value the value
     * @return a builder taking in consideration the new property
     */
    ObjectBuilder<T> property(String key, Object value);

    /**
     * <p>
     * Disables the support of a property with a particular value. Any future setting which match the given key/value pair
     * (when calling {@link #property(String, Object)}) will result in an {@link IllegalArgumentException}.
     * </p>
     *
     * @param key the key
     * @param value the value
     * @return a builder taking in consideration the new setting
     */
    ObjectBuilder<T> disableSupport(String key, Object value);

    /**
     * <p>
     * Gets the property identified by the given key.
     * </p>
     *
     * <p>
     * Throws an {@link IllegalArgumentException} if the property key is not supported.
     * </p>
     *
     * @param key the key
     * @return the value associated to the key
     */
    Object property(String key);

    /**
     * <p>
     * Gets the property {@code Map}.
     * </p>
     *
     * @return the properties
     */
    Map<String, Object> getProperties();

    /**
     * <p>
     * Builds a new {@link com.github.wuic.nut.dao.NutDao} thanks to the current state
     * of this builder.
     * </p>
     *
     * <p>
     * If the {@link com.github.wuic.nut.dao.NutDao} could not be built, then an {@link IllegalArgumentException}
     * should be thrown.
     * </p>
     *
     * @return the new {@link com.github.wuic.nut.dao.NutDao}
     */
    T build();
}
