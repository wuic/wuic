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


package com.github.wuic.servlet.test;

import com.github.wuic.servlet.HttpServletRequestAdapter;
import com.github.wuic.servlet.HttpServletResponseAdapter;
import com.github.wuic.util.CollectionUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * Tests for adapters. An adapter must call a wrapped instance if not {@code null} for each method from the implemented
 * interface.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
@RunWith(JUnit4.class)
public class HttpServletAdapterTest {

    /**
     * Timeout.
     */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    /**
     * Tests request adapter.
     *
     * @throws Exception if test fails
     */
    @Test
    public void requestTest() throws Exception {
        testAdapter(HttpServletRequestAdapter.class, HttpServletRequest.class);
    }

    /**
     * Tests response adapter.
     *
     * @throws Exception if test fails
     */
    @Test
    public void responseTest() throws Exception {
        testAdapter(HttpServletResponseAdapter.class, HttpServletResponse.class);
    }

    /**
     * <p>
     * Asserts that all methods declared in the given interface are called when a instance if wrapped inside the specified
     * adapter.
     * </p>
     *
     * @param adapter the adapter
     * @param wrappedType the interface wrapped by the adapter
     * @throws Exception if test fails
     */
    public void testAdapter(final Class<?> adapter, final Class<?> wrappedType) throws Exception {
        final Object mock = Mockito.mock(adapter);
        final Set<Method> invoked = new HashSet<Method>();

        // Collect called method from the wrapped instance
        for (final Method method : wrappedType.getMethods()) {
            final Object[] params = new Object[method.getParameterTypes().length];

            for (int i = 0; i < params.length; i++) {
                params[i] = Mockito.any(Class.class.cast(method.getParameterTypes()[i]));
            }

            Mockito.when(method.invoke(mock, params)).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    invoked.add(method);
                    return null;
                }
            });
        }

        final Object object = adapter.getConstructor(wrappedType).newInstance(mock);

        // Invoke methods
        for (final Method method : wrappedType.getMethods()) {
            final Object[] params = new Object[method.getParameterTypes().length];

            for (int i = 0; i < params.length; i++) {
                params[i] = Mockito.any(Class.class.cast(method.getParameterTypes()[i]));
            }

            Mockito.when(method.invoke(mock, params)).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    invoked.add(method);
                    return null;
                }
            });

            method.invoke(object, params);
        }

        // Check that all methods have been called
        final Set<Method> diff = CollectionUtils.difference(invoked, new HashSet<Method>(Arrays.asList(wrappedType.getMethods())));
        Assert.assertTrue(diff.toString(), diff.isEmpty());

        // Run empty
        final Object emptyObject = adapter.getConstructor().newInstance();

        // Invoke methods
        for (final Method method : wrappedType.getMethods()) {
            final Object[] params = new Object[method.getParameterTypes().length];

            for (int i = 0; i < params.length; i++) {
                params[i] = Mockito.any(Class.class.cast(method.getParameterTypes()[i]));
            }

            Mockito.when(method.invoke(mock, params)).thenAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return null;
                }
            });

            method.invoke(emptyObject, params);
        }
    }
}
