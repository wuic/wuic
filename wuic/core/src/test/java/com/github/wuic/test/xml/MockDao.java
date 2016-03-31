package com.github.wuic.test.xml;

import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.config.Config;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoListener;
import com.github.wuic.nut.dao.NutDaoService;
import com.github.wuic.util.FutureLong;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
     * <p>
     * Builds a new instance.
     * </p>
     * 
     * @param foo custom property
     */
    @Config
    public MockDao(@StringConfigParam(propertyKey = "c.g.dao.foo", defaultValue = "") String foo) {
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
        when(nut.getInitialNutType()).thenReturn(NutType.CSS);
        when(nut.getNutType()).thenReturn(NutType.CSS);
        when(nut.getName()).thenReturn("foo.css");
        when(nut.getInitialName()).thenReturn("foo.css");
        final List<Nut> nuts = new ArrayList<Nut>();
        nuts.add(nut);
        when(nut.openStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
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
    public void save(final Nut nut) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean saveSupported() {
        return true;
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
    public InputStream newInputStream(final String path, final ProcessContext processContext) throws IOException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean exists(final String path, final ProcessContext processContext) throws IOException {
        return null;
    }
}