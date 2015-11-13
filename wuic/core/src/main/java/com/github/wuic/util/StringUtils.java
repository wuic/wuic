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


package com.github.wuic.util;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Utility class providing helper constants and static methods around strings.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.4
 */
public final class StringUtils {

    /**
     * Hexadecimal possibles characters.
     */
    public static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * <p>
     * Prevent instantiation of this class which provides only static methods.
     * </p>
     */
    private StringUtils() {

    }

    /**
     * <p>
     * Converts bytes to hexadecimal {@code String}.
     * See: http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
     * </p>
     *
     * @param bytes bytes
     * @return hexadecimal string
     */
    public static String toHexString(final byte[] bytes) {
        char[] hexChars = new char[bytes.length * NumberUtils.TWO];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & NumberUtils.TWO_FIVE_FIVE;
            hexChars[j * NumberUtils.TWO] = HEX_ARRAY[v >>> NumberUtils.FOUR];
            hexChars[j * NumberUtils.TWO + 1] = HEX_ARRAY[v & NumberUtils.FIFTEEN];
        }

        return new String(hexChars);
    }

    /**
     * <p>
     * Simplifies the given string representation of a path by removing substring like '/foo/bar/../dir' by '/foo/dir',
     * considering the '..' substring as a reference to the parent path and the {@link IOUtils#STD_SEPARATOR} as separator.
     * </p>
     *
     * @param toSimplify the path to simplify
     * @return the simplified path, {@code null} if it is not possible to simplify
     * @see StringUtils#simplifyPathWithDoubleDot(String, String)
     */
    public static String simplifyPathWithDoubleDot(final String toSimplify) {
        return simplifyPathWithDoubleDot(toSimplify, IOUtils.STD_SEPARATOR);
    }

    /**
     * <p>
     * Simplifies the given string representation of a path by removing substring like '/foo/bar/../dir' by '/foo/dir',
     * considering the '..' substring as a reference to the parent path. In this case, the path separator must be '/'.
     * Not that the separator must not contain regex keywords.
     * </p>
     *
     * <p>
     * If a parent path is not found in the string, then {@code null} is returned. For instance, '../bar' can't be
     * simplified.
     * </p>
     *
     * @param toSimplify the path to simplify
     * @param separator the path separator
     * @return the simplified path, {@code null} if it is not possible to simplify
     */
    public static String simplifyPathWithDoubleDot(final String toSimplify, final String separator) {
        final String[] exploded = toSimplify.split(separator);

        // Manage "/foo/bar/" vs "/foo/bar" case
        final StringBuilder retval = new StringBuilder(toSimplify.endsWith(separator) ? separator : "");
        int countSkip = 0;

        for (int i = exploded.length - 1; i >= 0; i--) {
            final String path = exploded[i];

            if (path.isEmpty()) {
                // No parent to be read : return null
                if (i == 0 && countSkip > 0) {
                    return null;
                } else {
                    continue;
                }
            } else if (".".equals(path)) {
                continue;
            }

            // Parent path reference detected: could be simplified
            if ("..".equals(path)) {
                countSkip++;
            } else if (countSkip > 0) {
                countSkip--;

                if (retval.indexOf(separator) != separator.length() - 1) {
                    retval.insert(0, separator);
                }
            } else {
                retval.insert(0, path).insert(0, separator);
            }

            // We must know the parent
            if (countSkip > i) {
                // We don't know the parent : return null
                return null;
            }
        }

        // Manage "bar/foo/" vs "bar/foo" case
        if (!toSimplify.startsWith(separator)) {
            retval.delete(0, separator.length());
        }

        return retval.toString();
    }

    /**
     * <p>
     * Removes all trailing {@code String} at the beginning and at the end of the specified {@code String}.
     * </p>
     *
     * @param str the string to be treated
     * @param trailing the trailing string to remove
     * @return the str without trailing string
     */
    public static String removeTrailing(final String str, final String trailing) {
        if (trailing == null || trailing.isEmpty()) {
            return str;
        } else {
            String retval = str;

            while (retval.startsWith(trailing)) {
                retval = retval.substring(trailing.length());
            }

            while (retval.endsWith(trailing)) {
                retval = retval.substring(0, retval.length() - trailing.length());
            }

            return retval;
        }
    }

    /**
     * <p>
     * Merges all the given {@code String} into one {@code String} separated with the separator.
     * </p>
     *
     * <p>
     * For example, merge(new String[] { "foo", "oof", }, ":") results in "foo:oof".
     * </p>
     *
     * <p>
     * If the separator is {@code null}, then no separator is inserted. If only one is
     * specified, then this is directly returned. If no element is given, an empty {@code String}
     * will be returned.
     * </p>
     *
     * <p>
     * Finally, the method ensure that the separator has only one occurrence between words. For example :
     * merge(new String[] { "foo:", ":oof", }, ":") results in "foo:oof".
     * </p>
     *
     * @param merge to merge
     * @param separator the separator
     * @return the result of merge operation
     */
    public static String merge(final String[] merge, final String separator) {

        // One string and one separator, nothing to merge
        if (merge.length == 1) {
            return merge[0];
        } else {
            final StringBuilder retval = new StringBuilder();

            // At least two string needs to be merged
            if (merge.length > 1) {
                for (int i = 0; i < merge.length; i++)  {
                    final String value = merge[i];

                    // Do not append the separator if present at the beginning of the string
                    if (separator != null && i != 0 && value.startsWith(separator)) {
                        retval.append(value.substring(separator.length()));
                    } else {
                        retval.append(value);
                    }

                    // Do not append null, duplicate the separator or append one occurrence at the last position
                    if ((separator != null && i != merge.length - 1)
                            && !retval.toString().endsWith(separator)) {
                        retval.append(separator);
                    }
                }
            }

            return retval.toString();
        }
    }

    /**
     * <p>
     * Computes from the given collection of {@code String} the common path they start with.
     * </p>
     *
     * @param elements the collection
     * @return the beginning
     */
    public static String computeCommonPathBeginning(final Collection<String> elements) {

        // Nothing to compare
        if (elements.isEmpty()) {
            return "";
        } else if (elements.size() == 1) {
            // Only one element
            final String el = elements.iterator().next();
            final int lastIndex = el.lastIndexOf('/');
            return lastIndex == -1 ? "" : lastIndex == 0 ? "/" : el.substring(0, lastIndex);
        }

        final StringBuilder basePathBuilder = new StringBuilder();

        for (final String path : elements) {
            checkStartWith(basePathBuilder, path);
        }

        final String retval = basePathBuilder.toString();

        // Check that no path is finally not the build path itself
        for (final String path : elements) {
            if (path.equals(retval)) {
                return "";
            }
        }

        return basePathBuilder.toString();
    }

    /**
     * <p>
     * Checks that the path begin with the {@code String} of the given builder and adapt the builder if not.
     * </p>
     *
     * @param basePathBuilder the builder
     * @param path the path
     */
    public static void checkStartWith(final StringBuilder basePathBuilder, final String path) {
        int lastIndex = path.lastIndexOf('/');

        // Single path case
        if (lastIndex == -1) {
            lastIndex = path.length();
        }

        // First path?
        if (basePathBuilder.length() == 0) {
            basePathBuilder.append(path.substring(0, lastIndex));
        } else {
            // Keep character until one is different
            for (int i = 0; i < basePathBuilder.length() && i < path.length(); i++) {
                if (basePathBuilder.charAt(i) != path.charAt(i)) {
                    basePathBuilder.delete(i, basePathBuilder.length());

                    lastIndex = basePathBuilder.lastIndexOf("/");

                    // Remove partial path after '/'
                    if (lastIndex != -1 && i > lastIndex) {
                        basePathBuilder.delete(lastIndex, basePathBuilder.length());
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Handy method that replaces all the occurrences of the given {@code String} by a replacement in the string builder
     * specified in parameter.
     * </p>
     *
     * @param str the characters to replace
     * @param replacement the replacement
     * @param builder the builder to modify
     * @return the modified builder
     */
    public static StringBuilder replaceAll(final String str, final String replacement, final StringBuilder builder) {
        // last index
        int index = -1;

        // Replace each occurrence
        while ((index = builder.indexOf(str, index + 1)) != -1) {
            builder.replace(index, index + str.length(), replacement);
        }

        return builder;
    }

    /**
     * <p>
     * Returns a {@code String} corresponding to the sequences of characters contained in the given array between two
     * positions (expressed with two couples of integers line/column).
     * </p>
     *
     * @param lines the matrix
     * @param startLine the starting line in the matrix (min value is 1)
     * @param startCol the starting column in the matrix (min value is 1)
     * @param endLine the ending line in the matrix (min value is 1)
     * @param endCol the ending column in the matrix (min value is 1)
     * @return the resulting string
     */
    public static String substringMatrix(final String[] lines,
                                         final int startLine,
                                         final int startCol,
                                         final int endLine,
                                         final int endCol) {
        final StringBuilder retval = new StringBuilder();

        if (startLine == endLine) {
            // Only one line
            retval.append(lines[startLine - 1].substring(startCol - 1, endCol - 1));
        } else {
            // Copy from the beginning to the end of line
            retval.append(lines[startLine - 1].substring(startCol - 1));
            retval.append('\n');

            // Copy all plain lines between starting and ending lines
            for (int i = startLine + 1; i < endLine; i++) {
                retval.append(lines[i - 1]).append('\n');
            }

            // Copy from the first character to the ending column
            retval.append(lines[endLine - 1].substring(0, endCol - 1));
        }

        return retval.toString();
    }

    /**
     * <p>
     * Looks for the last position (line/column) of a sequence of character in the given matrix having a specified length
     * and starting at a particular position (line/column). The last position is reported by setting the values in the
     * two given {@code AtomicInteger}.
     * </p>
     *
     * @param lines the matrix
     * @param startLine the starting line
     * @param startColumn the starting column
     * @param length the length
     * @param endLine the ending line
     * @param endColumn the ending column
     */
    public static void reachEndLineAndColumn(final String[] lines,
                                             final int startLine,
                                             final int startColumn,
                                             final int length,
                                             final AtomicInteger endLine,
                                             final AtomicInteger endColumn) {
        int remain = length;

        for (int i = startLine - 1; i < lines.length; i++) {
            final String l = lines[i];

            // Count the correct number of columns if it does not starts at the first character of the first line
            final int len = (i == startLine - 1) ? l.length() - startColumn + 1 : l.length();

            if (len == remain) {
                // Last line reached (EOL)
                endLine.set(i + 1);
                endColumn.set(l.length());
                return;
            } else if (len > remain) {
                // Last line reached (!EOL)
                endLine.set(i + 1);
                endColumn.set(remain);
                return;
            } else {
                // Count the next line
                remain -= len;
            }
        }
    }
}
