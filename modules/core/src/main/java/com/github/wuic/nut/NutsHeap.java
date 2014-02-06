/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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
 * @version 1.6
 * @since 0.1.0
 */
public class NutsHeap implements NutDaoListener, HeapListener {

    /**
     * Message's template displayed when no nut has been found.
     */
    private static final String EMPTY_PATH_MESSAGE = "Path(s) %s retrieved with %s don't correspond to any physic nuts";

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    /**
     * The paths list.
     */
    private List<String> paths;

    /**
     * The nut types.
     */
    private Set<NutType> nutTypes;

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
    private final Set<HeapListener> listeners;

    /**
     * The ID identifying this heap.
     */
    private String id;

    /**
     * Heap composition.
     */
    private NutsHeap[] composition;

    /**
     * All the nuts created through through heap.
     */
    private Set<String> created;

    /**
     * <p>
     * Builds a heap by copy.
     * </p>
     *
     * @param other the heap to copy
     */
    public NutsHeap(final NutsHeap other) {
        this.id = other.id;
        this.listeners = other.listeners;
        this.composition = other.composition;
        this.nutDao = other.nutDao;
        this.nuts = other.nuts;
        this.nutTypes = other.nutTypes;
        this.paths = other.paths;
        this.created = other.created;
    }

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
        this.nutTypes = new HashSet<NutType>();
        this.created = new HashSet<String>();
        checkFiles();
    }

    /**
     * <p>
     * Finds the {@link NutDao} that created this nut.
     * </p>
     *
     * @param nut the nut
     * @return the nut's DAO, {@code null} if not found
     */
    public NutDao findDaoFor(final Nut nut) {
        final NutDao retval = recursiveFindDaoFor(nut);

        if (retval == null) {
            log.warn("Did not found any NutDao for nut {} inside heap {}", nut, this);
        }

        return retval;
    }

    /**
     * <p>
     * Finds the {@link NutDao} that created this nut recursively through the composition.
     * </p>
     *
     * @param nut the nut
     * @return the nut's DAO, {@code null} if not found
     */
    private NutDao recursiveFindDaoFor(final Nut nut) {
        // Heap has its own DAO, check inside first
        if (hasCreated(nut)) {
            return getNutDao();
        }

        // Search inside composition recursively
        for (final NutsHeap heap : getComposition()) {
            final NutDao retval = heap.recursiveFindDaoFor(nut);

            if (retval != null) {
                return retval;
            }
        }

        return null;
    }

    /**
     * <p>
     * Gets the {@link NutDao} of this {@link NutsHeap}. Caution, don't use its method like
     * {@link NutDao#create(String, com.github.wuic.nut.NutDao.PathFormat)}, {@link NutDao#proxyUriFor(Nut)} or
     * {@link NutDao#withRootPath(String)} but to the corresponding proxy methods in this class which check if the
     * {@link NutDao} to actually use is not in one of the {@link NutsHeap heaps} that compose this {@link NutsHeap}.
     * </p>
     *
     * @return the {@link NutDao}
     */
    public NutDao getNutDao() {
        return nutDao;
    }

    /**
     * <p>
     * Delegates method of {@link NutDao#proxyUriFor(Nut)}. The DAO is picked from heap or its composition.
     * </p>
     *
     * @param nut the nut
     * @return the proxy URI
     */
    public String proxyUriFor(final Nut nut) {
        final NutDao dao = findDaoFor(nut);
        return dao != null ? dao.proxyUriFor(nut) : null;
    }

    /**
     * <p>
     * Delegates method of {@link NutDao#withRootPath(String)}. The DAO is picked from heap or its composition.
     * </p>
     *
     * @param nut the root nut
     * @param rootPath the path
     * @return the proxy DAO
     */
    public NutDao withRootPath(final String rootPath, final Nut nut) {
        final NutDao dao = findDaoFor(nut);
        return dao != null ? dao.withRootPath(rootPath) : null;
    }

    /**
     * <p>
     * Delegates method of {@link NutDao#create(String, com.github.wuic.nut.NutDao.PathFormat)}.
     * The DAO is picked from heap or its composition.
     * </p>
     *
     * @param nut the root nut
     * @param path the nut path to create
     * @param pathFormat the format
     * @return the new nut
     * @throws StreamException if any I/O error occurs
     */
    public Map<Nut, Long> create(final Nut nut, final String path, final NutDao.PathFormat pathFormat) throws StreamException {
        final NutDao dao = findDaoFor(nut);
        final Map<Nut, Long> retval;

        if (dao != null) {
            retval = dao.create(path, pathFormat);
            nutDao.observe(path, this);

            for (final Nut n : retval.keySet()) {
                created.add(n.getName());
            }
        } else {
            retval = Collections.emptyMap();
        }

        return retval;
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
        synchronized (listeners) {
            this.listeners.add(listener);
        }
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
        // Keep order with a linked data structure
        this.nuts = new LinkedHashMap<Nut, Long>();

        log.info("Checking files for heap '{}'", id);

        if (paths != null) {
            for (final String path : paths) {
                final Map<Nut, Long> res = nutDao.create(path);
                nuts.putAll(res);
                nutDao.observe(path, this);

                for (final Nut nut : res.keySet()) {
                    created.add(nut.getName());
                }
            }
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
        for (final NutsHeap heap : getComposition()) {
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
        for (final Nut nut : toCheck) {
            nutTypes.add(NutType.getNutTypeForExtension(nut.getName().substring(nut.getName().lastIndexOf('.'))));
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
     * Gets the {@link NutType} types.
     * </p>
     * 
     * @return the types
     */
    public Set<NutType> getNutTypes() {
        return nutTypes;
    }

    /**
     * <p>
     * Sets the {@link NutDao}.
     * </p>
     *
     * @param dao the DAO
     */
    public void setNutDao(final NutDao dao) {
        nutDao = dao;
    }

    /**
     * <p>
     * Gets all the nuts of this heap.
     * </p>
     *
     * @return the nuts
     */
    public Set<Nut> getNuts() {
        final Set<Nut> retval = new LinkedHashSet<Nut>(nuts.keySet());

        for (final NutsHeap c : getComposition()) {
            retval.addAll(c.getNuts());
        }

        return retval;
    }

    /**
     * <p>
     * Gets all the nuts of this heap associated to their timestamp version.
     * </p>
     *
     * @return the nuts
     */
    public Map<Nut, Long> getNutsWithTimestamp() {
        final Map<Nut, Long> retval = new LinkedHashMap<Nut, Long>(nuts);

        for (final NutsHeap c : getComposition()) {
            retval.putAll(c.getNutsWithTimestamp());
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

        // Paths have not changed if difference is empty, otherwise we notify listeners
        boolean retval = CollectionUtils.difference(current, paths).isEmpty();

        if (!retval) {
            retval = notifyListeners();
        }

        return retval;
    }

    /**
     * <p>
     * Creates a new iterator on the {@link Nut nuts}.
     * </p>
     *
     * @return the iterator
     * @see NutsHeapIterator
     * @see CompositeIterator
     */
    public Iterator<List<Nut>> iterator() {
        return new CompositeIterator();
    }

    /**
     * <p>
     * Indicates if this {@link NutsHeap} has created thanks to its own DAO a {@link Nut}. The methods search for the
     * original nut if the specified nut is a generated one.
     * </p>
     *
     * @param nut the nut
     * @return {@code true} if heap has created a {@link Nut} with this path, {@code false} otherwise
     */
    public Boolean hasCreated(final Nut nut) {
        Nut refOrigin = nut;

        while (refOrigin.getOriginalNuts() != null && !refOrigin.getOriginalNuts().isEmpty()) {
            refOrigin = refOrigin.getOriginalNuts().get(0);
        }

        return created.contains(refOrigin.getName());
    }

    /**
     * <p>
     * Internal class that allow to iterate on this {@link NutsHeap heap} but also on the {@link NutsHeap heaps} composing it.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.3
     */
    private final class CompositeIterator implements Iterator<List<Nut>> {

        /**
         * The current index of the iterator inside the composition.
         */
        private int index;

        /**
         * The actual iterator.
         */
        private Iterator<List<Nut>> delegate;

        /**
         * Builds a new instance.
         */
        CompositeIterator() {
            delegate = new NutsHeapIterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            while (!delegate.hasNext() && index < composition.length) {
                delegate = composition[index++].iterator();
            }

            return delegate.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Nut> next() {
            return delegate.next();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * <p>
     * Internal class which helps iterating on all its {@link Nut nuts} including its composition. The {@link Nut nuts}
     * are read and returned by sequence of elements having the same {@link NutType}.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.3
     */
    private final class NutsHeapIterator implements Iterator<List<Nut>> {

        /**
         * Iterator.
         */
        private Iterator<Nut> iterator;

        /**
         * Next element.
         */
        private Nut next;

        /**
         * <p>
         * Builds a new instance by initializing the iterator.
         * </p>
         */
        NutsHeapIterator() {
            iterator = getNutsWithTimestamp().keySet().iterator();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Nut> next() {
            if (next == null) {
                next = iterator.next();
            }

            final LinkedList<Nut> retval = new LinkedList<Nut>();
            retval.add(next);

            while (iterator.hasNext()) {
                next = iterator.next();

                if (next.getNutType().equals(retval.getLast().getNutType())) {
                    retval.add(next);
                } else {
                    return retval;
                }
            }

            return retval;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
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
     * Returns the {@link NutsHeap heaps} that compose this instance.
     * </p>
     *
     * @return this composition
     */
    public final NutsHeap[] getComposition() {
        return composition == null ? new NutsHeap[0] : composition;
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
    public boolean notifyListeners(final NutsHeap observable) {
        try {
            // Will update the nuts
            checkFiles();

            synchronized (listeners) {
                for (final HeapListener l : listeners) {
                    l.nutUpdated(observable);
                }
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
