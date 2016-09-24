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


package com.github.wuic.test;

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.LineInspectorFactory;
import com.github.wuic.engine.LineInspectorListener;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.TimerTreeFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * A custom inspector for test purpose.
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class LIF extends LineInspector implements LineInspectorFactory {

    /**
     * Factory called.
     */
    public static boolean factoryCalled = false;

    /**
     * Inspector called.
     */
    public static boolean inspectorCalled = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LineInspector> create(final NutType nutType) {
        factoryCalled = true;
        return nutType.isBasedOn(EnumNutType.JAVASCRIPT) ? Arrays.asList((LineInspector) this) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newInspection() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inspect(final LineInspectorListener listener,
                        final char[] data,
                        final int offset,
                        final int length,
                        final EngineRequest request,
                        final CompositeNut.CompositeInput cis,
                        final ConvertibleNut originalNut)
            throws WuicException {
        inspectorCalled = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String toString(final TimerTreeFactory timerTreeFactory, final ConvertibleNut convertibleNut) throws IOException {
        return null;
    }
}
