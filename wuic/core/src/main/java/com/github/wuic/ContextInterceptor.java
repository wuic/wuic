/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.nut.ConvertibleNut;

import java.util.List;

/**
 * <p>
 * Represents an interceptor that is called just before and just after a workflow is processed. This gives a last chance
 * to modify input and output of several services. For instance, an interceptor could intercept an {@link EngineRequest}
 * the {@link Context} is going to use and the list of {@link ConvertibleNut nuts} it returns when the job is done.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
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
     * @param request the request
     * @return the request to use
     */
    EngineRequest beforeProcess(EngineRequest request);

    /**
     * <p>
     * Called just before the workflow is executed to retrieve one path.
     * </p>
     *
     * @param request the request
     * @param path the path
     * @return the request to use
     */
    EngineRequest beforeProcess(EngineRequest request, String path);

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
