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

/**
 * <p>
 * Utility class providing helper constants and static methods around numbers.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.3.3
 */
public final class NumberUtils {

    /**
     * Two. Use this constant to evict checkstyle issue.
     */
    public static final int TWO = 2;

    /**
     * Three. Use this constant to evict checkstyle issue.
     */
    public static final int THREE = 3;

    /**
     * Four. Use this constant to evict checkstyle issue.
     */
    public static final int FOUR = 4;

    /**
     * Six. Use this constant to evict checkstyle issue.
     */
    public static final int SIX = 6;

    /**
     * Height. Use this constant to evict checkstyle issue.
     */
    public static final int HEIGHT = 8;

    /**
     * Thirteen. Use this constant to evict checkstyle issue.
     */
    public static final int THIRTEEN = 13;

    /**
     * Fourteen. Use this constant to evict checkstyle issue.
     */
    public static final int FOURTEEN = 14;

    /**
     * Fifteen.
     */
    public static final int FIFTEEN = 15;

    /**
     * Two hundred and fifty five.
     */
    public static final int TWO_FIVE_FIVE = 255;

    /**
     * One thousand. Use this constant to evict checkstyle issue.
     */
    public static final int ONE_THOUSAND = 1000;

    /**
     * <p>
     * Prevent instantiation of this class which provides only static methods.
     * </p>
     */
    private NumberUtils() {

    }

    /**
     * <p>
     * Checks if the given {@code String} if a number (i.e eventually begins with '-' symbol and followed by digits).
     * </p>
     *
     * @param candidate the {@code String} to test
     * @return {@code true} if the {@code String} is a number, {@code false} otherwise
     */
    public static Boolean isNumber(final String candidate) {
        return candidate.replaceAll("-?\\d+", "").length() == 0;
    }
}
