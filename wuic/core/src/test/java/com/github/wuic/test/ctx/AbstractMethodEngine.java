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


package com.github.wuic.test.ctx;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract engine with methods annotated with {@link com.github.wuic.config.Config}.
 */
public abstract class AbstractMethodEngine extends NodeEngine {

    /**
     * A string that track constructor/method invocations.
     */
    protected String calls = "";

    /**
     * Overridden method.
     */
    @Config
    public void toOverride() {
        calls += "parent";
    }

    /**
     * Init method.
     */
    @Config
    public void c() {
        calls += "a";
    }

    /**
     * Init method.
     */
    @Config
    public void b() {
        calls += "b";
    }

    /**
     * Init method.
     */
    @Config
    public void a() {
        calls += "c";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(getNutTypeFactory().getNutType(EnumNutType.JAVASCRIPT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.AGGREGATOR;
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
        return null;
    }

    /**
     * <p>
     * Gets the calls.
     * </p>
     *
     * @return calls
     */
    public String getCalls() {
        return calls;
    }
}
