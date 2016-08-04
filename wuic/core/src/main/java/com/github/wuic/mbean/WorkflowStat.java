/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.mbean;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * Statistics objects for a particular workflow.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class WorkflowStat implements WorkflowStatMXBean {

    /**
     * The ID identifying the workflow.
     */
    private final String id;

    /**
     * All the executions statistics for this workflow.
     */
    private final List<WorkflowExecution> executions;

    /**
     * Maximum number of executions per statistic.
     */
    private final int maxExecutions;

    /**
     * Average duration of an execution.
     */
    private long averageDuration;

    /**
     * Total duration.
     */
    private long total;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param id the workflow ID
     * @param maxExecutions maximum number of executions per statistic
     */
    public WorkflowStat(final String id, final int maxExecutions) {
        this.id = id;
        this.executions = new LinkedList<WorkflowExecution>();
        this.averageDuration = -1;
        this.total = 0;
        this.maxExecutions = maxExecutions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowExecution> getExecutions() {
        return executions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAverageDuration() {
        return averageDuration;
    }

    /**
     * <p>
     * Adds a new executions.
     * </p>
     *
     * @param execution the execution
     */
    public void addExecution(final WorkflowExecution execution) {
        if (executions.size() == maxExecutions) {
            total -= executions.remove(0).getParseDuration();
        }

        executions.add(execution);
        total += execution.getParseDuration() + execution.getTransformDuration();
        averageDuration = total / executions.size();
    }
}
