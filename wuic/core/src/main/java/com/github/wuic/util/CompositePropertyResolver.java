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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class relies on a set of {@link PropertyResolver resolvers} to retrieve a key.
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class CompositePropertyResolver implements PropertyResolver {

    /**
     * The composition.
     */
    private final List<PropertyResolver> composition;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public CompositePropertyResolver() {
        this.composition = new ArrayList<PropertyResolver>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveProperty(final String key) {
        // Look into the composition
        for (final PropertyResolver resolver : composition) {
            final String result = resolver.resolveProperty(key);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * <p>
     * Adds a resolver to this composition.
     * The resolver is added in first position of the composition in order to be evaluated in first.
     * If a key exists in multiple placeholders, the composition will return the value found the first time.
     * </p>
     *
     * @param propertyResolver the new resolver
     */
    public void addPropertyResolver(final PropertyResolver propertyResolver) {
        composition.add(0, propertyResolver);
    }
}
