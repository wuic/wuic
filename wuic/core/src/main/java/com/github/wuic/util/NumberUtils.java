/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.util;

/**
 * <p>
 * Utility class providing helper constants and static methods around numbers.
 * </p>
 *
 * @author Guillaume DROUET
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
     * Five. Use this constant to evict checkstyle issue.
     */
    public static final int FIVE = 5;

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
     * Twenty.
     */
    public static final int TWENTY = 20;

    /**
     * Two hundred and fifty five.
     */
    public static final int TWO_FIVE_FIVE = 255;

    /**
     * One thousand. Use this constant to evict checkstyle issue.
     */
    public static final int ONE_THOUSAND = 1000;

    /**
     * One thousand and twenty four. Number of bytes in a kilobyte.
     */
    public static final int ONE_KB = 1024;

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
        return !candidate.isEmpty() && candidate.replaceAll("-?\\d+", "").length() == 0;
    }

    /**
     * <p>
     * Computes the remaining length for a given offset based on an original offset and its associated length.
     * </p>
     *
     * @param originalOffset the original offset
     * @param originalLength the original length
     * @param actualOffset the actual offset
     * @return the actual length
     */
    public static int remainingLength(final int originalOffset, final int originalLength, final int actualOffset) {
        return originalLength - (actualOffset - originalOffset);
    }
}
