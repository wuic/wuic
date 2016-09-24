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

import com.github.wuic.mbean.WorkflowExecution;

/**
 * <p>
 * This class represents a heap execution event with the information associated to the workflow ID.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class WorkflowExecutionEvent {

    /**
     * The workflow ID.
     */
    private String id;

    /**
     * The execution.
     */
    private WorkflowExecution execution;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param id the workflow id
     * @param execution the execution
     */
    public WorkflowExecutionEvent(final String id, final WorkflowExecution execution) {
        this.id = id;
        this.execution = execution;
    }

    /**
     * <p>
     * Gets the workflow ID.
     * </p>
     *
     * @return the workflow ID
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Gets the execution information.
     * </p>
     *
     * @return the execution information
     */
    public WorkflowExecution getExecution() {
        return execution;
    }
}
