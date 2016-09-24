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


package com.github.wuic.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * A {@link PropertyResolver} that allows an internal {@code Map} used as property repository to be populated via JMX.
 * When a property is resolved, the value in the {@code Map} will be used if it exists, otherwise {@code null} is returned.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class JmxPropertyResolver implements PropertyResolver, JmxPropertyResolverMXBean {

    /**
     * Overridden properties are stored in this internal {@code Map}.
     */
    private final Map<String, String> override;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public JmxPropertyResolver() {
        this.override = Collections.synchronizedMap(new HashMap<String, String>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProperty(final String key, final String value) {
        override.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveProperty(final String key) {
        return override.containsKey(key) ? override.get(key) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getProperties() {
        synchronized (override) {
            return Collections.unmodifiableMap(new HashMap<String, String>(override));
        }
    }
}
