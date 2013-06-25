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


package com.github.wuic.util;

/**
 * <p>
 * Utility class providing helper constants and static methods around strings.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.4
 */
public final class StringUtils {

    /**
     * <p>
     * Prevent instantiation of this class which provides only static methods.
     * </p>
     */
    private StringUtils() {

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
                    if (separator != null && value.startsWith(separator)) {
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
}
