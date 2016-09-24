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

import java.util.Map;

/**
 * <p>
 * A {@link PropertyResolver} that relies on a {@code Map}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class MapPropertyResolver implements PropertyResolver {

    /**
     * The map.
     */
    private final Map<Object, Object> properties;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param properties the map
     */
    public MapPropertyResolver(final Map<Object, Object> properties) {
        this.properties = properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveProperty(final String key) {
        return properties.containsKey(key) ? String.valueOf(properties.get(key)) : null;
    }
}
