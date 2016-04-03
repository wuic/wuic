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


package com.github.wuic.test;

import com.github.wuic.util.CompositePropertyResolver;
import com.github.wuic.util.EnhancedPropertyResolver;
import com.github.wuic.util.MapPropertyResolver;
import com.github.wuic.util.PropertyResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Test {@link com.github.wuic.util.PropertyResolver} implementations.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@RunWith(JUnit4.class)
public class PropertyResolverTest {

    /**
     * Tests {@link MapPropertyResolver}.
     */
    @Test
    public void mapTest() {
        final Map<Object, Object> map = new HashMap<Object, Object>();
        map.put("1", "2");
        final PropertyResolver resolver = new MapPropertyResolver(map);
        Assert.assertNull(resolver.resolveProperty("2"));
        Assert.assertEquals("2", resolver.resolveProperty("1"));
        Assert.assertNotNull(resolver.getKeys());
        Assert.assertEquals(1, resolver.getKeys().size());
        Assert.assertEquals("1", resolver.getKeys().iterator().next());
    }

    /**
     * Tests resolve in {@link MapPropertyResolver}.
     */
    @Test
    public void resolveOrderInCompositionTest() {
        final PropertyResolver r1 = Mockito.mock(PropertyResolver.class);
        Mockito.when(r1.resolveProperty("A")).thenReturn("1");
        Mockito.when(r1.resolveProperty("C")).thenReturn("r1");
        Mockito.when(r1.getKeys()).thenReturn(Arrays.asList((Object) "A", "C"));

        final PropertyResolver r2 = Mockito.mock(PropertyResolver.class);
        Mockito.when(r2.resolveProperty("B")).thenReturn("2");
        Mockito.when(r2.resolveProperty("C")).thenReturn("r2");
        Mockito.when(r2.getKeys()).thenReturn(Arrays.asList((Object) "B", "C"));

        final CompositePropertyResolver propertyResolver = new CompositePropertyResolver();
        propertyResolver.addPropertyResolver(r1);
        propertyResolver.addPropertyResolver(r2);

        Assert.assertEquals("1", propertyResolver.resolveProperty("A"));
        Assert.assertEquals("2", propertyResolver.resolveProperty("B"));
        Assert.assertEquals("r2", propertyResolver.resolveProperty("C"));

        Assert.assertEquals(3, propertyResolver.getKeys().size());
    }

    /**
     * Tests resolve placeholder in {@link com.github.wuic.util.EnhancedPropertyResolver}.
     */
    @Test
    public void resolvePlaceholderInEnhancerTest() {
        final PropertyResolver r1 = Mockito.mock(PropertyResolver.class);
        Mockito.when(r1.resolveProperty("A")).thenReturn("${B}");
        Mockito.when(r1.resolveProperty("B")).thenReturn("A");

        final PropertyResolver r2 = Mockito.mock(PropertyResolver.class);
        Mockito.when(r2.resolveProperty("C")).thenReturn("${B}");
        Mockito.when(r2.resolveProperty("D")).thenReturn("${A} ${C}");
        final EnhancedPropertyResolver propertyResolver = new EnhancedPropertyResolver();
        propertyResolver.addPropertyResolver(r1);
        propertyResolver.addPropertyResolver(r2);

        Assert.assertEquals("A", propertyResolver.resolveProperty("A"));
        Assert.assertEquals("A", propertyResolver.resolveProperty("B"));
        Assert.assertEquals("A", propertyResolver.resolveProperty("C"));
        Assert.assertEquals("${B} ${B}", propertyResolver.resolveProperty("D"));
        Assert.assertEquals(null, propertyResolver.resolveProperty("E"));
    }

    /**
     * Tests resolve holder with default value in {@link com.github.wuic.util.EnhancedPropertyResolver}.
     */
    @Test
    public void resolvePlaceholderDefaultInEnhancerTest() {
        final PropertyResolver r = Mockito.mock(PropertyResolver.class);
        Mockito.when(r.resolveProperty("A")).thenReturn("${B:C}");
        Mockito.when(r.resolveProperty("B")).thenReturn("B");
        Mockito.when(r.resolveProperty("C")).thenReturn("${D:E}");

        final EnhancedPropertyResolver propertyResolver = new EnhancedPropertyResolver();
        propertyResolver.addPropertyResolver(r);

        Assert.assertEquals("B", propertyResolver.resolveProperty("A"));
        Assert.assertEquals("B", propertyResolver.resolveProperty("B"));
        Assert.assertEquals("E", propertyResolver.resolveProperty("C"));
    }
}
