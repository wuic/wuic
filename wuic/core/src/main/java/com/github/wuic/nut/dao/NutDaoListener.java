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


package com.github.wuic.nut.dao;

import java.util.Set;

/**
 * <p>
 * This interface represents a listener which expects to be notified of changes when they occur on a particular
 * nut.
 * </p>
 *
 * <p>
 * To be notified, it must be registered to the {@link NutDao} thanks to its  {@link NutDao#observe(String, NutDaoListener...)}
 * method.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public interface NutDaoListener {

    /**
     * <p>
     * When a DAO performs a polling operation, this method is called first with all path which are going to be polled.
     * </p>
     *
     * <p>
     * The paths are computed thanks to their representation (regex or not) specified when this listener has been
     * registered to the observable.
     * </p>
     *
     * <p>
     * If the listener returns {@code false}, the observable will stop notifying it <b>only</b> during the polling
     * operation.
     * </p>
     *
     * @param pattern the pattern (actually the path itself or a regex) corresponding to the paths
     * @param paths all the real paths which are going to be polled
     * @return {@code false} if polling should not be done, {@code true} otherwise
     */
    boolean polling(String pattern, Set<String> paths);

    /**
     * <p>
     * Called when a nut has been polled.
     * </p>
     *
     * <p>
     * When the observable object checks in one operation the changes on a set of nuts, the listener could notifies the
     * observable to stop notifying it <b>only</b> during the operation.
     * </p>
     *
     * @param dao the DAO which polls
     * @param path the polled path
     * @param timestamp the timestamp retrieved when poling the nut
     * @return {@code true} if this listener needs to be notified of any other update during the observable's operation,
     * {@code false} otherwise
     */
    boolean nutPolled(NutDao dao, String path, Long timestamp);

    /**
     * <p>
     * Indicates if this listener should be removed from the observable object when a call to
     * {@link #polling(String, java.util.Set)} or {@link #nutPolled(NutDao, String, Long)} returns {@code false}.
     * </p>
     *
     * <p>
     * If a listener is going to be removed, all the other listeners created by the same {@link #getFactory() factory}
     * must be removed if and only if they are also disposable.
     * </p>
     *
     * @return {@code true} if this listener is disposable a described, {@code false} otherwise
     */
    boolean isDisposable();

    /**
     * <p>
     * An arbitrary object which helps to know how the listener has been created.
     * </p>
     *
     * @return the factory object
     */
    Object getFactory();
}
