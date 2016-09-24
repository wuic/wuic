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

import com.github.wuic.Workflow;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.nut.ConvertibleNut;

import java.util.List;

/**
 * <p>
 * Represents an interceptor that is called just before and just after a workflow is processed. This gives a last chance
 * to modify input and output of several services.
 * </p>
 *
 * <p>
 * For instance, an interceptor could intercept an {@link EngineRequestBuilder} the {@link Context} is going to use and
 * the list of {@link ConvertibleNut nuts} it returns when the job is done.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public interface ContextInterceptor {

    /**
     * <p>
     * Called just before if going to be retrieved with a specified ID.
     * </p>
     *
     * @param workflowId the retrieved workflow ID
     * @return the workflow ID to actually retrieve
     */
    String beforeGetWorkflow(String workflowId);

    /**
     * <p>
     * Called just after a workflow has been retrieved.
     * </p>
     *
     * @param id the workflow ID
     * @param workflow the retrieved workflow
     * @return the workflow to return
     */
    Workflow afterGetWorkflow(String id, Workflow workflow);


    /**
     * <p>
     * Called just before the workflow is executed.
     * </p>
     *
     * @param request the request builder
     * @return the request to use
     */
    EngineRequestBuilder beforeProcess(EngineRequestBuilder request);

    /**
     * <p>
     * Called just before the workflow is executed to retrieve one path.
     * </p>
     *
     * @param request the request builder
     * @param path the path
     * @return the request to use
     */
    EngineRequestBuilder beforeProcess(EngineRequestBuilder request, String path);

    /**
     * Called just after the workflow has been executed.
     *
     * @param nuts the nuts
     * @return the nuts the {@link Context} should return or give to the next interceptor
     */
    List<ConvertibleNut> afterProcess(List<ConvertibleNut> nuts);

    /**
     * Called just after the workflow has been executed to retrieve one path.
     *
     * @param nut the nut
     * @param path the path
     * @return the nut the {@link Context} should return or give to the next interceptor
     */
    ConvertibleNut afterProcess(ConvertibleNut nut, String path);
}
