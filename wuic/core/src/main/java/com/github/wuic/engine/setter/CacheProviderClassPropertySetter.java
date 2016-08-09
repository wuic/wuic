/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.engine.setter;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.PropertySetter;
import com.github.wuic.engine.CacheProvider;
import com.github.wuic.exception.WuicException;

/**
 * <p>
 * Setter for the {@link com.github.wuic.ApplicationConfig#CACHE_PROVIDER_CLASS} property.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
public class CacheProviderClassPropertySetter extends PropertySetter.PropertySetterOfObject {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void set(final Object value) {
        if (value == null) {
            put(getPropertyKey(), value);
        } else if (value instanceof String) {
            try {
                final Class<?> clazz = Class.forName(value.toString());

                if (CacheProvider.class.isAssignableFrom(clazz)) {
                    put(getPropertyKey(), CacheProvider.class.cast(clazz.newInstance()).getCache());
                } else {
                    WuicException.throwBadArgumentException(new IllegalArgumentException(
                            String.format("'%s' must be a '%s'", value.toString(), CacheProvider.class.getName())));
                }
            } catch (ClassNotFoundException cnfe) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(cnfe));
            } catch (InstantiationException ie) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(ie));
            } catch (IllegalAccessException iae) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(iae));
            }
        } else {
            WuicException.throwBadArgumentException(new IllegalArgumentException(
                    String.format("Value '%s' associated to key '%s' must be a String", value, getPropertyKey())));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyKey() {
        return ApplicationConfig.CACHE_PROVIDER_CLASS;
    }
}
