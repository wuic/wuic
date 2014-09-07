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


package com.github.wuic.util;

import java.util.*;

/**
 * <p>
 * Utility class built on top of the java collection framework helping WUIC to deal with
 * collections.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.1
 */
public final class CollectionUtils {

    /**
     * Empty {@link List} typed with {@code String}.
     */
    @SuppressWarnings("unchecked")
    public static final List<String> EMPTY_STRING_LIST = Collections.EMPTY_LIST;

    /**
     * <p>
     * Prevent instantiation of this class which provides only static methods.
     * </p>
     */
    private CollectionUtils() {

    }

    /**
     * <p>
     * Returns an array without the element at given indexes.
     * </p>
     *
     * @param source the source array
     * @param target array to recopy (length should equals source.length - exclusions.length)
     * @param exclusions the indexes to exclude (must appear in natural order)
     */
    public static <T> void without(T[] source, T[] target, final int ... exclusions) {
        int cpt = 0;

        for (int i = 0; i < source.length; i++) {
            if (Arrays.binarySearch(exclusions, i) < 0) {
                target[cpt++] = source[i];
            }
        }
    }

    /**
     * <p>
     * Returns a {@code Map} that keep the orders of its keys.
     * </p>
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     * @return the map
     */
    public static <K, V> Map<K, V> orderedKeyMap() {
        return new LinkedHashMap<K, V>();
    }

    /**
     * <p>
     * Returns a {@code List} with a initial capacity which equals to the number of
     * elements given in parameter and with those elements already added.
     * </p>
     *
     * @param elements the elements to add to the list to create
     * @param <T> the generic of the list
     * @return the creates {@code List}
     */
    public static <T> List<T> newList(final T ... elements) {
        final List<T> retval = new ArrayList<T>(elements.length);
        Collections.addAll(retval, elements);
        return retval;
    }

    /**
     * <p>
     * Returns a set containing all the elements which are not contained by the two given sets.
     * </p>
     *
     * @param first the first set
     * @param second the second set
     * @param <T> the type of element
     * @return the difference between two sets
     */
    public static <T> Set<T> difference(final Set<T> first, final Set<T> second) {
        final Set<T> retval = new HashSet<T>();

        for (final T e : first) {
            if (!second.contains(e)) {
                retval.add(e);
            }
        }

        for (final T e : second) {
            if (!first.contains(e)) {
                retval.add(e);
            }
        }

        return retval;
    }

    /**
     * <p>
     * Just the varargs version of {@link Arrays#deepHashCode(Object[])}
     * </p>
     *
     * @param objects the array to compute hash code
     * @return the hash code returned by {@link Arrays#deepHashCode(Object[])}
     */
    public static int deepHashCode(final Object ... objects) {
        return Arrays.deepHashCode(objects);
    }

    /**
     * <p>
     * Gets the index of a targetted element in a specified array.
     * </p>
     *
     * @param target the element to search
     * @param elements the array
     * @return the index of the element in the array, -1 if not found
     */
    public static int indexOf(final Object target, final Object ... elements) {
        for (int i = 0; i < elements.length; i++) {
            if (target.equals(elements[i])) {
                return i;
            }
        }

        return -1;
    }
}
