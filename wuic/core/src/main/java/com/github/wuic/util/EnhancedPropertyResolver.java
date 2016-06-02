/*
 * "Copyright (c) 2016   Capgemini Technology Services (final hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (final the "Software"), to use, copy, modify and
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
 * open source software licenses (final BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic.util;

/**
 * <p>
 * An enhanced {@link PropertyResolver}. Three main features are provided:
 * <ul>
 * <li>The class wraps an instance {@link CompositePropertyResolver} to provide composition support</li>
 * <li>The class adds a derived method called {@link #resolveProperty(String, String)} that returns a default value instead of {@code null}</li>
 * <li>Resolver properties values are transformed with {@link StringUtils#injectPlaceholders(String, PropertyResolver)}</li>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class EnhancedPropertyResolver implements PropertyResolver {

    /**
     * The wrapper composition.
     */
    private final CompositePropertyResolver composite;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public EnhancedPropertyResolver() {
        this.composite = new CompositePropertyResolver();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveProperty(final String key) {
        return StringUtils.injectPlaceholders(composite.resolveProperty(key), composite);
    }

    /**
     * <p>
     * Resolves a key and returns a default value if {@link #resolveProperty(String)} returns {@code null}.
     * </p>
     *
     * @param key the key
     * @param defaultValue the default value
     * @return the value, or the default value if the key is not resolved
     */
    public String resolveProperty(final String key, final String defaultValue) {
        final String retval = resolveProperty(key);
        return retval == null ? defaultValue : retval;
    }

    /**
     * <p>
     * Adds the given {@link PropertyResolver} to the wrapped {@link CompositePropertyResolver}.
     * </p>
     *
     * @param propertyResolver the new resolver
     */
    public void addPropertyResolver(final PropertyResolver propertyResolver) {
        composite.addPropertyResolver(propertyResolver);
    }
}
