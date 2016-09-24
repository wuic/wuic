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


package com.github.wuic.config.bean.json;

import com.github.wuic.ProcessContext;
import com.github.wuic.config.bean.WuicBean;
import com.github.wuic.exception.WuicException;
import com.google.gson.Gson;

import java.io.IOException;

/**
 * <p>
 * This configurator extracts the bean from a JSON object.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class JsonContextBuilderConfigurator extends BeanContextBuilderConfigurator {

    /**
     * Unmarshaller.
     */
    private final Gson gson;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param processContext the process context
     */
    protected JsonContextBuilderConfigurator(final String tag, final ProcessContext processContext) {
        this(Boolean.TRUE, tag, processContext);
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param tag the tag
     * @param multiple {@code true} if multiple configurations with the same tag could be executed, {@code false} otherwise
     * @param pc the process context
     */
    protected JsonContextBuilderConfigurator(final Boolean multiple, final String tag, final ProcessContext pc) {
        super(multiple, tag, pc);
        this.gson = new Gson();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicBean getWuicBean() throws WuicException {
        try {
            return unmarshal(gson);
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
            return null;
        }
    }

    /**
     * <p>
     * Unmashal the {@link com.github.wuic.config.bean.WuicBean} with the given {@code Gson} object.
     * </p>
     *
     * @param gson the unmarshaller
     * @return the unmarshalled bean
     * @throws IOException if any I/O error occurs
     */
    protected abstract WuicBean unmarshal(final Gson gson) throws IOException;
}
