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

import com.github.wuic.Logging;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Future;

/**
 * <p>
 * This class is able to schedule a polling operation in the {@link WuicScheduledThreadPool}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.0
 * @param <T> the type of listener
 */
public abstract class PollingScheduler<T> implements Runnable {

    /**
     * Polling interval in seconds (-1 to disable).
     */
    private int pollingInterval;

    /**
     * Help to know when a polling operation is done.
     */
    private Future<?> pollingResult;

    /**
     * All observers per listener.
     */
    private final Map<T, Polling> nutObservers;

    /**
     * Creates a new instance.
     */
    public PollingScheduler() {
        nutObservers = new WeakHashMap<T, Polling>();
    }

    /**
     * <p>
     * Adds a set of listeners to be notified when a polling operation is perform on a nut matching the given pattern.
     * </p>
     *
     * @param pattern the pattern to use to retrieve the different real paths to poll
     * @param listeners some listeners to be notified when an update has been detected on a nut
     * @throws IOException if an I/O occurs while retrieving last update of the nut
     */
    public final void observe(final String pattern, final T ... listeners) throws IOException {
        synchronized (getNutObservers()) {
            for (final T listener : listeners) {
                Polling polling = getNutObservers().get(listener);

                if (polling == null) {
                    polling = new Polling(listener);
                    nutObservers.put(listener, polling);
                }

                polling.addPattern(pattern);
            }
        }
    }

    /**
     * <p>
     * Gets polling data with observers.
     * </p>
     *
     * @return the observers
     */
    public Map<T, ? extends Polling> getNutObservers() {
        return nutObservers;
    }

    /**
     * <p>
     * Returns the polling interval.
     * </p>
     *
     * @return the polling interval
     */
    public final int getPollingInterval() {
        return pollingInterval;
    }

    /**
     * <p>
     * Defines a new polling interval. If current polling operation are currently processed, then they are not interrupted
     * and a new scheduling is created if the given value is a positive number. If the value is not positive, then no
     * polling will occur.
     * </p>
     *
     * @param intervalSeconds interval in seconds
     */
    public final synchronized void setPollingInterval(final int intervalSeconds) {

        // Stop current scheduling
        if (pollingResult != null) {
            Logging.POLL.log("Cancelling repeated polling operation for {}", getClass().getName());
            pollingResult.cancel(false);
            pollingResult = null;
        }

        pollingInterval = intervalSeconds;

        // Create new scheduling if necessary
        if (pollingInterval > 0) {
            Logging.POLL.log("Start polling operation for {} repeated every {} seconds", getClass().getName(), pollingInterval);
            pollingResult = WuicScheduledThreadPool.getInstance().executeEveryTimeInSeconds(this, pollingInterval);
        } else {
            Logging.POLL.log("Won't perform any polling operation for {}", getClass().getName());
        }
    }

    /**
     * <p>
     * Retrieves a timestamp that indicates the last time this nut has changed.
     * </p>
     *
     * @param path the real path of the nut
     * @return the timestamp
     * @throws IOException if any I/O error occurs
     */
    protected abstract Long getLastUpdateTimestampFor(final String path) throws IOException;

    /**
     * <p>
     * This class represents a polling information. It's composed of a listener to be notified polling is performed and
     * a set of patterns matching the desired nuts.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.1
     * @since 0.4.0
     */
    public class Polling {

        /**
         * Listener.
         */
        private WeakReference<T> listener;

        /**
         * The patterns.
         */
        private Set<String> patterns;

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param l the listener to be notified
         */
        public Polling(final T l) {
            listener = new WeakReference<T>(l);
            patterns = new HashSet<String>();
        }

        /**
         * <p>
         * Gets the patterns.
         * </p>
         *
         * @return the patterns
         */
        public Set<String> getPatterns() {
            return patterns;
        }

        /**
         * <p>
         * Adds all the specified listeners.
         * </p>
         *
         * @param pattern a new pattern matching the desired nuts
         */
        public void addPattern(final String pattern) {
            patterns.add(pattern);
        }

        /**
         * <p>
         * Gets the listener.
         * </p>
         *
         * @return the listener
         */
        public T getListener() {
            return listener.get();
        }
    }
}