package com.github.wuic.test.xml;

import com.github.wuic.NutType;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Mocked engine builder.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@EngineService(injectDefaultToWorkflow = false)
public class MockEngine extends NodeEngine {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param foo custom property
     */
    @ConfigConstructor
    public MockEngine(@StringConfigParam(propertyKey = "c.g.engine.foo", defaultValue = "") String foo) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return new ArrayList<NutType>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return Mockito.mock(EngineType.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        return request.getNuts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return true;
    }
}
