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


package com.github.wuic.context;

import com.github.wuic.ProcessContext;

/**
 * <p>
 * Partial implementation of the {@link ContextBuilderConfigurator} providing tag and process context.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class AbstractContextBuilderConfigurator extends ContextBuilderConfigurator {

    /**
     * The tag.
     */
    private final String tag;

    /**
     * The process context.
     */
    private final ProcessContext processContext;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param multiple {@code true} if multiple configurations with the same tag could be executed, {@code false} otherwise
     * @param tag the tag
     * @param processContext the process context
     */
    public AbstractContextBuilderConfigurator(final Boolean multiple, final String tag, final ProcessContext processContext) {
        super(multiple);
        this.tag = tag;
        this.processContext = processContext == null ? ProcessContext.DEFAULT : processContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ProcessContext getProcessContext() {
        return processContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getTag() {
        return tag;
    }
}
