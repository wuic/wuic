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


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <p>
 * This class represents a set of {@link Nut nuts}. Each {@link Nut} is built thanks to an associated {@link NutDao}
 * which only needs a particular path to create it.
 * </p>
 *
 * <p>
 * A path is an abstract location of one to many nuts because it could be a regular expression. It is relative to
 * the base path of the {@link NutDao}.
 * </p>
 *
 * <p>
 * All the paths must refer to the same type fo path (CSS, JS, etc).
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.4
 * @since 0.1.0
 */
public class NutsHeap implements NutDaoListener, HeapListener {

    /**
     * Message's template displayed when no nut has been found.
     */
    private static final String EMPTY_PATH_MESSAGE = "Path(s) %s retrieved with %s don't correspond to any physic nuts";

    /**
     * Message's template displayed when the extensions of nuts path is not correct.
     */
    private static final String BAD_EXTENSIONS_MESSAGE = "Bad extension for nut %s associated to the NutType %s";

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * The paths list.
     */
    private List<String> paths;

    /**
     * The nut type.
     */
    private NutType nutType;

    /**
     * The nut DAO.
     */
    private NutDao nutDao;

    /**
     * The nuts corresponding to the paths associated to their last update.
     */
    private Map<Nut, Long> nuts;

    /**
     * Listeners.
     */
    private Set<HeapListener> listeners;

    /**
     * The ID identifying this heap.
     */
    private String id;

    /**
     * Heap composition.
     */
    private NutsHeap[] composition;

    /**
     * <p>
     * Builds a new {@link NutsHeap}. All the paths must be named with an
     * extension that matches the {@link com.github.wuic.NutType}. If it is not the case, then
     * an {@link BadArgumentException} will be thrown.
     * </p>
     *
     * <p>
     * Some additional heaps could be specified to make a composition.
     * </p>
     * 
     * @param pathsList the paths
     * @param theNutDao the {@link NutDao}
     * @param heapId the heap ID
     * @param heaps some other heaps that compose this heap
     * @throws StreamException if the HEAP could not be created
     */
    public NutsHeap(final List<String> pathsList,
                    final NutDao theNutDao,
                    final String heapId,
                    final NutsHeap ... heaps) throws StreamException {
        this.id = heapId;
        this.paths = pathsList;
        this.nutDao = theNutDao;
        this.listeners = new HashSet<HeapListener>();
        this.composition = heaps;
        checkFiles();
    }

    /**
     * <p>
     * Observes all the nuts in this heap with the given listener.
     * </p>
     *
     * @param listener the listener
     * @throws StreamException if an I/O error occurs
     */
    public void addObserver(final HeapListener listener) throws StreamException {
        this.listeners.add(listener);
    }

    /**
     * <p>
     * Checks that the {@link com.github.wuic.NutType} and the paths list of this heap are not
     * null. If they are, this methods will throw an {@link BadArgumentException}.
     * This exception could also be thrown if one path of the list does have a name
     * which ends with one of the possible {@link com.github.wuic.NutType#extensions extensions}.
     * </p>
     *
     * @throws StreamException in I/O error case
     */
    private void checkFiles() throws StreamException {
        this.nuts = new HashMap<Nut, Long>();

        for (final String path : paths) {
            nuts.putAll(nutDao.create(path));
            nutDao.observe(path, this);
        }

        // Non null assertion
        if (paths == null && composition.length == 0) {
            throw new BadArgumentException(new IllegalArgumentException("A heap must have a non-null paths list and a non-empty composition"));
        // Do not allow empty heaps
        } else if (nuts.isEmpty() && composition.length == 0) {
            final String merge = StringUtils.merge(paths.toArray(new String[paths.size()]), ", ");
            throw new BadArgumentException(new IllegalArgumentException(String.format(EMPTY_PATH_MESSAGE, merge, nutDao.toString())));
        }

        // Check the extension of each path : all of them must share the same nut type
        checkExtension(nuts.keySet());

        // Also check other heaps and observe them
        for (final NutsHeap heap : composition) {
            checkExtension(heap.nuts.keySet());
            heap.addObserver(this);
        }
    }

    /**
     * <p>
     * Checks the extension of the given set. Makes sure that all nuts share the same type.
     * </p>
     *
     * @param toCheck set to check.
     */
    private void checkExtension(final Set<Nut> toCheck) {
        for (final Nut res : toCheck) {
            // Extract name to be test
            final String nut = res.getName();

            Boolean valid = Boolean.FALSE;

            // Apply test for each possible extension
            for (final NutType nt : NutType.values()) {
                for (final String extension : nt.getExtensions()) {
                    if (nut.endsWith(extension)) {
                        if (nutType == null) {
                            nutType = nt;
                        }

                        valid = nt.equals(nutType);
                    }
                }
            }

            // The path has not one of the possible extension : throw an IAE
            if (!valid) {
                final String message = String.format(BAD_EXTENSIONS_MESSAGE, nut, nutType);
                throw new BadArgumentException(new IllegalArgumentException(message));
            }
        }
    }

    /**
     * <p>
     * Gets the heap's ID.
     * </p>
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link NutType}.
     * </p>
     * 
     * @return the type
     */
    public NutType getNutType() {
        return nutType;
    }

    /**
     * <p>
     * Gets the {@link NutDao}.
     * </p>
     * 
     * @return the nut DAO
     */
    public NutDao getNutDao() {
        return nutDao;
    }

    /**
     * <p>
     * Gets all the nuts of this heap.
     * </p>
     *
     * @return the nuts
     */
    public Set<Nut> getNuts() {
        final Set<Nut> retval = new HashSet<Nut>(nuts.keySet());

        for (final NutsHeap c : composition) {
            retval.addAll(c.getNuts());
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean polling(final Set<String> paths) {
        final Set<String> current = new HashSet<String>();

        for (final Nut nut : nuts.keySet()) {
            current.add(nut.getName());
        }

        // Paths have not changed if different is empty, otherwise we notify listeners
        return CollectionUtils.difference(current, paths).isEmpty() || notifyListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nutPolled(final NutDao dao, final String path, final Long timestamp) {
        for (final Map.Entry<Nut, Long> entry : nuts.entrySet()) {
            // Nut has changed
            if (entry.getKey().getName().equals(path) && !entry.getValue().equals(timestamp)) {
                // We don't need to be notified anymore
                return notifyListeners();
            }
        }

        return true;
    }

    /**
     * <p>
     * Notifies the listeners that this heap has detected an update in one or many nuts.
     * </p>
     *
     * @return {@link false} for convenient usage in caller
     */
    private boolean notifyListeners() {
        return notifyListeners(this);
    }

    /**
     * <p>
     * Notifies the listeners that this heap has detected an update in one or many nuts.
     * </p>
     *
     * @param observable the heap where change has been detected
     * @return {@link false} for convenient usage in caller
     */
    private boolean notifyListeners(final NutsHeap observable) {
        try {
            // Will update the nuts
            checkFiles();
    
            for (final HeapListener l : listeners) {
                l.nutUpdated(observable);
            }
        } catch (StreamException se) {
            log.error("Unable to update nuts in the heap", se);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nutUpdated(final NutsHeap heap) {
        notifyListeners(heap);
    }
}
