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


package com.github.wuic;

/**
 * <p>
 * A configurator is called by a WUIC bootstrap in charge of {@link Context} creation using a {@link ContextBuilder}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public interface ContextBuilderConfigurator {

    /**
     * <p>
     * Configures the given context. Should {@link ContextBuilder#tag(String) tag} its configurations to change it
     * when it needs to update stuffs.
     * </p>
     *
     * @param cxtBuilder the builder
     */
    void configure(ContextBuilder cxtBuilder);

    /**
     * <p>
     * If this configurator polls its configurations to see check changes, then it should returns a positive number of
     * seconds to wait before the next polling operation.
     * </p>
     *
     * <p>
     * If changes are detected, then configurations should be updated in the {@link ContextBuilder} specified when its
     * method {@link ContextBuilderConfigurator#configure(ContextBuilder)} method has been called. The use of
     * {@link ContextBuilder#clearTag(String)} will be required to erase obsolete configuration for this configurator.
     * If {@link ContextBuilderConfigurator#configure(ContextBuilder)} has never been called, nothing will be done.
     * </p>
     *
     * @return the number of seconds before next polling, -1 if not polling will be performed
     */
    int nextPoll();
}
