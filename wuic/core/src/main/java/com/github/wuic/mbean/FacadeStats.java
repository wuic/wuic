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


package com.github.wuic.mbean;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A facade statistics exposes transformation and resolution information.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class FacadeStats implements FacadeStatsMXBean {

    /**
     * The resolution statistics.
     */
    private final List<HeapStat> heapStats;

    /**
     * The transformation statistics.
     */
    private final List<WorkflowStat> workflowStats;

    /**
     * Maximum number of workflow executions statistics.
     */
    private final int maxWorkflowExecutions;

    /**
     * Maximum number of heap resolutions statistics.
     */
    private final int maxHeapResolutions;

    /**
     * The number of times the facade has been refreshed.
     */
    private int refreshCount;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param maxWorkflowExecutions maximum number of workflow executions statistics
     * @param maxHeapResolutions maximum number of heap resolutions statistics
     */
    public FacadeStats(final int maxWorkflowExecutions, final int maxHeapResolutions) {
        this.heapStats = new ArrayList<HeapStat>();
        this.workflowStats = new ArrayList<WorkflowStat>();
        this.maxWorkflowExecutions = maxWorkflowExecutions;
        this.maxHeapResolutions = maxHeapResolutions;
        this.refreshCount = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HeapStat> getHeapStats() {
        return heapStats;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRefreshCount() {
        return refreshCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowStat> getWorkflowStats() {
        return workflowStats;
    }

    /**
     * <p>
     * Increments the refresh count.
     * </p>
     */
    public void addRefreshCount() {
        this.refreshCount++;
    }

    /**
     * <p>
     * Adds an resolution statistics for the heap identified by the given ID.
     * </p>
     *
     * @param heapResolution the statistics
     * @param heapId the workflow ID
     */
    public void addHeapResolution(final HeapResolution heapResolution, final String heapId) {
       HeapStat heap = null;

        // Check if statistics for the given ID already exists
        for (final HeapStat heapStat : heapStats) {
            if (heapId.equals(heapStat.getId())) {
                heap = heapStat;
                break;
            }
        }

        // First statistics for this heap
        if (heap == null) {
            heap = new HeapStat(heapId, maxHeapResolutions);
            heapStats.add(heap);
        }

        heap.addResolution(heapResolution);
    }

    /**
     * <p>
     * Adds an execution statistics for the workflow identified by the given ID.
     * </p>
     *
     * @param workflowExecution the statistics
     * @param workflowId the workflow ID
     */
    public void addWorkflowExecution(final WorkflowExecution workflowExecution, final String workflowId) {
        WorkflowStat workflow = null;

        // Check if statistics for the given ID already exists
        for (final WorkflowStat workflowStat : workflowStats) {
            if (workflowId.equals(workflowStat.getId())) {
                workflow = workflowStat;
                break;
            }
        }

        // First statistics for this workflow
        if (workflow == null) {
            workflow = new WorkflowStat(workflowId, maxWorkflowExecutions);
            workflowStats.add(workflow);
        }

        workflow.addExecution(workflowExecution);
    }
}

