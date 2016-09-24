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


package com.github.wuic.test.xml;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.Config;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoListener;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.util.DefaultInput;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.Input;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * Mocked DAO builder.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@NutDaoService
public class MockDao implements NutDao {

    /**
     * Foo value.
     */
    private final String foo;

    /**
     * Bar value.
     */
    private final String bar;

    /**
     * Baz value.
     */
    private final String baz;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     * 
     * @param foo custom property
     */
    @Config
    public MockDao(@StringConfigParam(propertyKey = "c.g.dao.foo", defaultValue = "") String foo,
                   @StringConfigParam(propertyKey = "c.g.dao.bar", defaultValue = "") String bar,
                   @StringConfigParam(propertyKey = "c.g.dao.baz", defaultValue = "baz") String baz) {
        this.foo = foo;
        this.bar = bar;
        this.baz = baz;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void observe(final String realPath, final NutDaoListener... listeners) throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final ProcessContext processContext) throws IOException {
        return create(path, null, processContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> create(final String path, final PathFormat format, final ProcessContext processContext) throws IOException {
        final ConvertibleNut nut = mock(ConvertibleNut.class);
        when(nut.getInitialNutType()).thenReturn(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));
        when(nut.getNutType()).thenReturn(new NutType(EnumNutType.CSS, Charset.defaultCharset().displayName()));
        when(nut.getName()).thenReturn("foo.css");
        when(nut.getInitialName()).thenReturn("foo.css");
        final List<Nut> nuts = new ArrayList<Nut>();
        nuts.add(nut);
        when(nut.openStream()).thenReturn(new DefaultInput(new ByteArrayInputStream(new byte[0]), Charset.defaultCharset().displayName()));
        when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));
        return nuts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String proxyUriFor(final Nut nut) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutDao withRootPath(final String rootPath) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input newInputStream(final String path, final ProcessContext processContext) throws IOException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        return null;
    }

    /**
     * <p>
     * Gets foo.
     * </p>
     *
     * @return foo
     */
    public String getFoo() {
        return foo;
    }

    /**
     * <p>
     * Gets bar.
     * </p>
     *
     * @return bar
     */
    public String getBar() {
        return bar;
    }

    /**
     * <p>
     * Gets baz.
     * </p>
     *
     * @return baz
     */
    public String getBaz() {
        return baz;
    }
}