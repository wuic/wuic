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


package com.github.wuic.test.nut;

import com.github.wuic.ProcessContext;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;

import java.io.IOException;
import java.util.Arrays;

import com.github.wuic.nut.Source;
import com.github.wuic.nut.SourceImpl;
import com.github.wuic.nut.SourceMapNutImpl;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.core.ProxyNutDao;

import com.github.wuic.util.NutUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.mockito.Mockito;

/**
 * <p>
 * Nut utils tests.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.1
 */
@RunWith(JUnit4.class)
public class NutUtilTest {

    /**
     * Tests for redirection performed by {@link ProxyNutDao}.
     *
     * @throws IOException if test fails
     */
    @Test(timeout = 60000)
    public void proxyTest() throws IOException {
        final NutDao delegate = Mockito.mock(NutDao.class);
        final Nut delegateNut = Mockito.mock(Nut.class);
        Mockito.when(delegateNut.getInitialName()).thenReturn("delegate");
        Mockito.when(delegate.create(Mockito.anyString(), Mockito.any(NutDao.PathFormat.class), Mockito.any(ProcessContext.class))).thenReturn(Arrays.asList(delegateNut));

        final Nut proxyNut = Mockito.mock(Nut.class);
        Mockito.when(proxyNut.getInitialName()).thenReturn("nut");

        final NutDao proxyDao = Mockito.mock(NutDao.class);
        final Nut proxyDaoNut = Mockito.mock(Nut.class);
        Mockito.when(proxyDaoNut.getInitialName()).thenReturn("dao");
        Mockito.when(proxyDao.create(Mockito.anyString(), Mockito.any(NutDao.PathFormat.class), Mockito.any(ProcessContext.class))).thenReturn(Arrays.asList(proxyDaoNut));

        final ProxyNutDao proxy = new ProxyNutDao("", delegate);
        proxy.addRule("nut", proxyNut);
        proxy.addRule("dao", proxyDao);

        Assert.assertNotNull(proxy.create("delegate", null, ProcessContext.DEFAULT));
        Assert.assertEquals(1, proxy.create("delegate", null, ProcessContext.DEFAULT).size());
        Assert.assertEquals("delegate", proxy.create("delegate", null, ProcessContext.DEFAULT).get(0).getInitialName());

        Assert.assertNotNull(proxy.create("dao", null, ProcessContext.DEFAULT));
        Assert.assertEquals(1, proxy.create("dao", null, ProcessContext.DEFAULT).size());
        Assert.assertEquals("dao", proxy.create("dao", null, ProcessContext.DEFAULT).get(0).getInitialName());

        Assert.assertNotNull(proxy.create("nut", null, ProcessContext.DEFAULT));
        Assert.assertEquals(1, proxy.create("nut", null, ProcessContext.DEFAULT).size());
        Assert.assertEquals("nut", proxy.create("nut", null, ProcessContext.DEFAULT).get(0).getInitialName());
    }

    /**
     * Tests source lookup.
     */
    @Test(timeout = 60000)
    public void searchSourceTest() {
        final ConvertibleNut nut = Mockito.mock(ConvertibleNut.class);
        final SourceMapNutImpl src = Mockito.mock(SourceMapNutImpl.class);
        Mockito.when(nut.getSource()).thenReturn(src);
        Mockito.when(src.getName()).thenReturn("source");
        Mockito.when(nut.getName()).thenReturn("nut");
        Assert.assertNotNull(NutUtils.findByName(nut, "source"));
    }
}
