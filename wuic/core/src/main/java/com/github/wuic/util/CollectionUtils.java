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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Utility class built on top of the java collection framework helping WUIC to deal with
 * collections.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.1
 */
public final class CollectionUtils {

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
     * The set will be a {@link LinkedHashSet} if one parameter is an instance of this class, otherwise a {@link HashSet}
     * will be returned.
     * </p>
     *
     * @param first the first set
     * @param second the second set
     * @param <T> the type of element
     * @return the difference between two sets
     */
    public static <T> Set<T> difference(final Set<T> first, final Set<T> second) {
        final Set<T> retval = first instanceof LinkedHashSet || second instanceof LinkedHashMap ?
                new LinkedHashSet<T>() : new HashSet<T>();

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
     * Merges the given source to the target specified in parameter by creating new entries when the key in the target
     * does not exists or in the other case by adding all the element of the list associated to the key to the existing
     * value.
     * </p>
     *
     * @param source the source
     * @param target the target
     * @param <E> list element type
     * @param <K> the key type
     */
    public static <E, K> void merge(final Map<K, List<E>> source, final Map<K, List<E>> target) {
        for (final Map.Entry<K, List<E>> entry : source.entrySet()) {
            merge(entry.getKey(), entry.getValue(), target);
        }
    }

    /**
     * <p>
     * Adds the given key/value pair to the given {@code Map} as a new entry if it does not already contain the key.
     * In the other case, the all the elements of the given value are added to the {@code List} associated to the current
     * value.
     * </p>
     *
     * @param key the key
     * @param value the value
     * @param target the target
     * @param <E> list element type
     * @param <K> the key type
     */
    public static <E, K> void merge(final K key, List<E> value, final Map<K, List<E>> target) {
        if (target.containsKey(key)) {
            target.get(key).addAll(value);
        } else {
            target.put(key, value);
        }
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
