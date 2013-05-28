/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.configuration.Configuration;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.resource.WuicResource;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * Simple composition of engines.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.3
 */
public class CGCompositeEngine extends Engine {

    /**
     * The engines of this composition.
     */
    private Engine[] engines;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param e the non-null and non-empty array of engines (should share the same configuration)
     */
    public CGCompositeEngine(final Engine... e) {
        if (e == null || e.length == 0) {
            throw new IllegalArgumentException("A composite engine must be built with a non-null and non-empty array of engines");
        }

        engines = e;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> parse(final EngineRequest request) throws IOException {
         List<WuicResource> retval = request.getResources();

        for (Engine engine : engines) {
            retval = engine.parse(new EngineRequest(retval, request.getContextPath(), request.getGroup()));
        }

        if (getNext() != null) {
            retval = getNext().parse(new EngineRequest(retval, request.getContextPath(), request.getGroup()));
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration() {
        return engines[0].getConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return engines[0].works();
    }
}
