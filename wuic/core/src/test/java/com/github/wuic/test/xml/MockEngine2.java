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
import com.github.wuic.NutTypeFactory;
import com.github.wuic.config.Config;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.InMemoryInput;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 * Mocked engine builder.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@EngineService(injectDefaultToWorkflow = false)
public class MockEngine2 extends MockEngine {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    @Config
    public MockEngine2() {
        super("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();
        retval.addAll(request.getNuts());
        final ConvertibleNut nut = mock(ConvertibleNut.class);
        when(nut.getInitialNutType()).thenReturn(new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS));
        when(nut.getNutType()).thenReturn(new NutTypeFactory(Charset.defaultCharset().displayName()).getNutType(EnumNutType.CSS));
        when(nut.getName()).thenReturn("bar.css");
        when(nut.getInitialName()).thenReturn("bar.css");
        when(nut.getVersionNumber()).thenReturn(new FutureLong(1L));

        try {
            when(nut.openStream()).thenReturn(new InMemoryInput(new byte[0], Charset.defaultCharset().displayName()));
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
        }

        retval.add(nut);

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return getNutTypeFactory().getNutType(EnumNutType.values());
    }
}
