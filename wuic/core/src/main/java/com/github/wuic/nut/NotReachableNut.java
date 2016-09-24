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


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.mbean.TransformationStat;
import com.github.wuic.util.FutureLong;
import com.github.wuic.util.Input;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.TimerTreeFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This class represents a nut that is not reachable. Usually instantiated by the
 * {@link com.github.wuic.engine.core.StaticEngine}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.1
 */
public class NotReachableNut extends AbstractConvertibleNut {

    /**
     * The nut's workflow.
     */
    private String workflow;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param name the nut name
     * @param nutType the nut type
     * @param workflowId the nut's workflow
     * @param version the version number
     */
    public NotReachableNut(final String name, final NutType nutType, final String workflowId, final Long version) {
        super(name, nutType, new FutureLong(version), Boolean.FALSE);
        workflow = workflowId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input openStream() throws IOException {
        WuicException.throwNutNotFoundException(getInitialName(), workflow);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<TransformationStat>> transform(final Pipe.OnReady... onReady) throws IOException {
        WuicException.throwNutNotFoundException(getInitialName(), workflow);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<TransformationStat>> transform(final TimerTreeFactory timerTreeFactory, final Pipe.OnReady... onReady)
            throws IOException {
        WuicException.throwNutNotFoundException(getInitialName(), workflow);
        return null;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public boolean isTransformed() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParentFile() {
        return null;
    }
}
