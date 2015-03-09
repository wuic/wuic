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


package com.github.wuic.nut;

import com.github.wuic.NutType;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.dao.NutDaoListener;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * @version 1.7
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
     * The nuts corresponding to the paths.
     */
    private List<Nut> nuts;

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
    private Map<String, Set<String>> created;

    /**
     * Disposable heap as described in {@link NutDaoListener#isDisposable()} or not.
     */
    private final boolean disposable;

    /**
     * Factory object as described in {@link NutDaoListener#getFactory()} and {@link NutDaoListener#isDisposable()}.
     */
    private final Object factory;

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
        this.nutDao = other.nutDao;
        this.nuts = other.nuts;
        this.nutTypes = other.nutTypes;
        this.paths = other.paths;
        this.created = other.created != null ? other.created : new HashMap<String, Set<String>>();
        this.disposable = other.disposable;
        this.factory = other.factory;

        if (other.composition != null) {
            this.composition = new NutsHeap[other.composition.length];

            for (int i = 0; i < this.composition.length; i++) {
                composition[i] = new NutsHeap(other.composition[i]);
            }
        }
    }

    /**
     * <p>
     * Builds a new {@link NutsHeap}. All the paths must be named with an
     * extension that matches the {@link com.github.wuic.NutType}. If it is not the case, then
     * an {@link IllegalArgumentException} will be thrown.
     * </p>
     *
     * <p>
     * Some additional heaps could be specified to make a composition.
     * </p>
     *
     * @param factoryObject the factory object (see {@link #factory})
     * @param isDisposable is disposable or not (see {@link #disposable}
     * @param pathsList the paths
     * @param theNutDao the {@link NutDao}
     * @param heapId the heap ID
     * @param heaps some other heaps that compose this heap
     * @throws java.io.IOException if the HEAP could not be created
     */
    public NutsHeap(final Object factoryObject,
                    final List<String> pathsList,
                    final boolean isDisposable,
                    final NutDao theNutDao,
                    final String heapId,
                    final NutsHeap ... heaps) throws IOException {
        this.factory = factoryObject;
        this.id = heapId;
        this.paths = pathsList == null ? new ArrayList<String>() : new ArrayList<String>(pathsList);
        this.nutDao = theNutDao;
        this.listeners = new HashSet<HeapListener>();
        this.composition = heaps;
        this.nutTypes = new HashSet<NutType>();
        this.created = new HashMap<String, Set<String>>();
        this.disposable = isDisposable;
        checkFiles();
    }

    /**
     * <p>
     * Builds a heap which is not disposable.
     * </p>
     *
     * @param factoryObject the factory object ({@link #factory})
     * @param pathsList the paths
     * @param theNutDao the {@link NutDao}
     * @param heapId the heap ID
     * @param heaps some other heaps that compose this heap
     * @throws java.io.IOException if the HEAP could not be created
     */
    public NutsHeap(final Object factoryObject,
            final List<String> pathsList,
            final NutDao theNutDao,
            final String heapId,
            final NutsHeap ... heaps) throws IOException {
        this(factoryObject, pathsList, false, theNutDao, heapId, heaps);
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
        final NutsHeap retval = findHeapFor(nut);
        return retval == null ? null : retval.getNutDao();
    }

    /**
     * <p>
     * Finds the {@link NutsHeap} that created this nut.
     * </p>
     *
     * @param nut the nut
     * @return the nut's HEAP, {@code null} if not found
     */
    public NutsHeap findHeapFor(final Nut nut) {
        final NutsHeap retval = recursiveFindHeapFor(nut);

        if (retval == null) {
            log.warn("Did not found any NutDao for nut {} inside heap {}", nut, this);
        }

        return retval;
    }

    /**
     * <p>
     * Finds the {@link NutsHeap} that created this nut recursively through the composition.
     * </p>
     *
     * @param nut the nut
     * @return the nut's DAO, {@code null} if not found
     */
    private NutsHeap recursiveFindHeapFor(final Nut nut) {
        // Heap has its own DAO, check inside first
        if (hasCreated(nut)) {
            return this;
        }

        // Search inside composition recursively
        for (final NutsHeap heap : getComposition()) {
            final NutsHeap retval = heap.recursiveFindHeapFor(nut);

            if (retval != null) {
                return retval;
            }
        }

        return null;
    }

    /**
     * <p>
     * Gets the {@link NutDao} of this {@link NutsHeap}. Caution, don't use its method like
     * {@link NutDao#create(String, com.github.wuic.nut.dao.NutDao.PathFormat)}, {@link NutDao#proxyUriFor(Nut)} or
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
     * Delegates method of {@link NutDao#create(String, com.github.wuic.nut.dao.NutDao.PathFormat)}.
     * The DAO is picked from heap or its composition.
     * </p>
     *
     * @param nut the root nut
     * @param path the nut path to create
     * @param pathFormat the format
     * @return the new nut
     * @throws IOException if any I/O error occurs
     */
    public List<Nut> create(final Nut nut, final String path, final NutDao.PathFormat pathFormat) throws IOException {
        final NutsHeap heap = findHeapFor(nut);
        final List<Nut> retval;

        if (heap != null && heap.getNutDao() != null) {
            retval = heap.getNutDao().create(path, pathFormat);

            if (!retval.isEmpty()) {
                // Check if a root path is appended to the nut name
                final String rootPath = heap.getNutDao() instanceof AbstractNutDao.WithRootPathNutDao ?
                        AbstractNutDao.WithRootPathNutDao.class.cast(heap.getNutDao()).getRootPath() : null;
                heap.getNutDao().observe(rootPath == null ? path : IOUtils.mergePath(rootPath, path), this);

                for (final Nut n : retval) {
                    addCreate(path, n.getInitialName());
                }
            }
        } else {
            retval = Collections.emptyList();
        }

        return retval;
    }

    /**
     * <p>
     * Observes all the nuts in this heap with the given listener.
     * </p>
     *
     * @param listener the listener
     */
    public void addObserver(final HeapListener listener)  {
        synchronized (listeners) {
            this.listeners.add(listener);
        }
    }

    /**
     * <p>
     * Refers a path as a resource created by this heap for the given pattern.
     * </p>
     *
     *
     * @param pattern the pattern the is generated for
     * @param path the created path resource
     */
    public final void addCreate(final String pattern, final String path) {
        Set<String> p = created.get(pattern);

        if (p == null) {
            p = new HashSet<String>();
            created.put(pattern, p);
        }

        p.add(path);
    }

    /**
     * <p>
     * Checks that the {@link com.github.wuic.NutType} and the paths list of this heap are not
     * null. If they are, this methods will throw an {@link IllegalArgumentException}.
     * This exception could also be thrown if one path of the list does have a name
     * which ends with one of the possible {@link com.github.wuic.NutType#extensions extensions}.
     * </p>
     *
     * @throws IOException in I/O error case
     */
    private void checkFiles() throws IOException {
        // Keep order with a linked data structure
        this.nuts = new ArrayList<Nut>();

        log.info("Checking files for heap '{}'", id);

        if (paths != null) {
            for (final String path : paths) {
                created.remove(path);
                final List<Nut> res = nutDao.create(path);
                nuts.addAll(res);
                nutDao.observe(path, this);

                for (final Nut nut : res) {
                    addCreate(path, nut.getInitialName());

                    final int startIndex = nut.getInitialName().charAt(0) == '/' ? 1 : 0;
                    final int slashIndex = nut.getInitialName().indexOf('/', startIndex);

                    if (slashIndex != -1 && NumberUtils.isNumber(nut.getInitialName().substring(startIndex, slashIndex))) {
                        WuicException.throwBadArgumentException(new IllegalArgumentException(
                                String.format("First level of nut name's path cannot be a numeric value: %s",
                                        nut.getInitialName())));
                    }
                }
            }
        }

        // Non null assertion
        if (paths == null && composition.length == 0) {
            WuicException.throwBadArgumentException(
                    new IllegalArgumentException("A heap must have a non-null paths list and a non-empty composition"));
        // Do not allow empty heaps
        } else if (nuts.isEmpty() && composition.length == 0) {
            final String merge = StringUtils.merge(paths.toArray(new String[paths.size()]), ", ");
            WuicException.throwBadArgumentException(
                    new IllegalArgumentException(String.format(EMPTY_PATH_MESSAGE, merge, nutDao.toString())));
        }

        // Check the extension of each path : all of them must share the same nut type
        checkExtension(nuts);

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
     * @param toCheck set to check
     */
    private void checkExtension(final Collection<Nut> toCheck) {
        for (final Nut nut : toCheck) {
            nutTypes.add(NutType.getNutTypeForExtension(nut.getInitialName().substring(nut.getInitialName().lastIndexOf('.'))));
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
     * Sets the {@link NutDao} for the heap that created the specified {@link NutDao}.
     * </p>
     *
     * @param dao the DAO
     * @param nut the created nut
     */
    public void setNutDao(final NutDao dao, final Nut nut) {
        final NutsHeap heap = findHeapFor(nut);

        if (heap != null) {
            heap.nutDao = dao;
        }
    }

    /**
     * <p>
     * Gets all the nuts of this heap.
     * </p>
     *
     * @return the nuts
     */
    public List<Nut> getNuts() {
        final List<Nut> retval = new ArrayList<Nut>(nuts);

        for (final NutsHeap c : getComposition()) {
            retval.addAll(c.getNuts());
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean polling(final String pattern, final Set<String> paths) {
        boolean retval = true;

        try {
            final Set<String> current = created.get(pattern);

            if (current != null) {
                // Paths have not changed if difference is empty, otherwise we notify listeners
                final Collection<String> diff = CollectionUtils.difference(current, paths);
                retval = diff.isEmpty();

                if (!retval) {
                    log.info("Nut(s) added and/or removed in heap {}: {}", id, Arrays.toString(diff.toArray()));
                    checkFiles();
                    retval = notifyUpdateToListeners();
                }
            }
        } catch (IOException se) {
            log.error("Unable to update nuts in the heap", se);
        }

        return retval;
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
        final Nut refOrigin;

        if (ConvertibleNut.class.isAssignableFrom(nut.getClass())) {
            ConvertibleNut convertibleNut = ConvertibleNut.class.cast(nut);

            while (convertibleNut.getOriginalNuts() != null && !convertibleNut.getOriginalNuts().isEmpty()) {
                convertibleNut = convertibleNut.getOriginalNuts().get(0);
            }

            refOrigin = convertibleNut;
        } else {
            refOrigin = nut;
        }

        for (final Set<String> p : created.values()) {
            if (p.contains(refOrigin.getInitialName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * Indicates if the given heap equals to this heap or any heap of its composition. The equality if evaluated
     * recursively through the composition.
     * </p>
     *
     * @param heap the heap to compare
     * @return {@code true} if its equals to this heap or to a heap of its composition
     */
    public boolean containsHeap(final NutsHeap heap) {
        if (equals(heap)) {
            return true;
        } else {
            if (composition != null) {
                for (final NutsHeap h : composition) {
                    if (h.containsHeap(heap)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean nutPolled(final NutDao dao, final String path, final Long timestamp) {
        try {
            for (final Nut nut : nuts) {
                // Nut has changed
                if (nut.getInitialName().equals(path) && !NutUtils.getVersionNumber(nut).equals(timestamp)) {
                    checkFiles();
                    // We don't need to be notified anymore
                    return notifyUpdateToListeners();
                }
            }

            return true;
        } catch (IOException se) {
            log.error("Unable to update nuts in the heap", se);
            return true;
        }
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
     * @return {@code false} for convenient usage in caller
     */
    private boolean notifyUpdateToListeners() {
        return notifyListeners(this);
    }

    /**
     * <p>
     * Notifies the listeners that this heap has detected an update in one or many nuts.
     * </p>
     *
     * @param observable the heap where change has been detected
     * @return {@code false} for convenient usage in caller
     */
    public boolean notifyListeners(final NutsHeap observable) {
        synchronized (listeners) {
            for (final HeapListener l : listeners) {
                l.nutUpdated(observable);
            }
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDisposable() {
        return disposable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getFactory() {
        return factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (o instanceof NutsHeap) {
            final NutsHeap heap = NutsHeap.class.cast(o);
            return heap.id.equals(id) && CollectionUtils.difference(new HashSet<Object>(paths), new HashSet<Object>(heap.paths)).isEmpty();
        }

        return false;
    }

    /**
     * <p>
     * The {@link NutsHeap} is a {@link NutDaoListener} registered in the {@link NutDao}. Because the listeners are
     * registered in weak references, we must make sure a strong reference prevents the objects from garbage collection.
     * This class helps to keep a strong reference to the registered listener.
     * </p>
     *
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.1
     */
    public static enum ListenerHolder {

        /**
         * Singleton.
         */
        INSTANCE;

        /**
         * The strong references.
         */
        private final List<HeapListener> listeners;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         */
        private ListenerHolder() {
            listeners = new ArrayList<HeapListener>();
        }

        /**
         * <p>
         * Adds a strong reference to the listener.
         * </p>
         *
         * @param l the listener
         */
        public void add(final HeapListener l) {
            synchronized (listeners) {
                listeners.add(l);
            }
        }

        /**
         * <p>
         * Clears all strong references.
         * </p>
         */
        public void clear() {
            synchronized (listeners) {
                listeners.clear();
            }
        }
    }
}
