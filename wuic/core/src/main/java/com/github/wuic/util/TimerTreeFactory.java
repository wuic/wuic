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


package com.github.wuic.util;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This factory produces special {@link Timer} allowing to deduct from the elapsed time the time spent by sub timers.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class TimerTreeFactory {

    /**
     * The current timer tree.
     */
    private TimerTree timerTree;

    /**
     * <p>
     * Gets a new timer tree. If a timer tree already exists, the new timer tree will be a child of it and is referenced
     * until a new child is created or the {@link TimerTree#end()} is called.
     * </p>
     *
     * @return the {@code TimerTree}
     */
    public TimerTree getTimerTree() {
        if (timerTree == null) {
            timerTree = new TimerTree(null);
        } else {
            timerTree = timerTree.newChild();
        }

        return timerTree;
    }

    /**
     * <p>
     * A {@code TimerTree} is a special timer that has some timers created while it's already started. Once the time
     * elapsed of a {@code TimerTree} is retrieved, the time elapsed from all children is deducted from the returned value.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    class TimerTree extends Timer {

        /**
         * The parent.
         */
        private TimerTree parent;

        /**
         * All the ended timer of the children that need to be deducted from its own elapsed time.
         */
        private Map<TimerTree, Long> deduct;

        /**
         * <p>
         * Builds a new instance with a parent.
         * </p>
         *
         * @param parent the parent, {@code null} if this is a root
         */
        private TimerTree(final TimerTree parent) {
            this.parent = parent;
            this.deduct = new HashMap<TimerTree, Long>();
        }

        /**
         * <p>
         * Creates a new timer referenced as a children of this instance.
         * </p>
         *
         * @return the timer
         */
        private TimerTree newChild() {
            return new TimerTree(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long end() {
            long elapsed = super.end();

            // Deduct this time from parent
            if (parent != null) {
                parent.deduct.put(this, elapsed);
            }

            // Deduct the time spent by the children
            for (final Long t : deduct.values()) {
                elapsed -= t;
            }

            // Give hand to the parent
            if (timerTree == this) {
                timerTree = this.parent;
            }

            return elapsed;
        }
    }
}
