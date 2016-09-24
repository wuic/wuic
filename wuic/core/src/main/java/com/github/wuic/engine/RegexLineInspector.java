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


package com.github.wuic.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * {@link java.util.regex.Pattern} based {@link LineInspector}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class RegexLineInspector extends LineMatcherInspector {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(LineInspector.class);

    /**
     * The pattern.
     */
    private Pattern pattern;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param p the pattern.
     */
    public RegexLineInspector(final Pattern p) {
        pattern = p;
        logger.info("LineInspector {} will be inspected with pattern '{}'", getClass().getName(), p.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MatcherAdapter lineMatcher(final String line) {
        return new MatcherAdapter(line);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newInspection() {
        // Nothing to do
    }

    /**
     * <p>
     * Creates a {@link Matcher} thanks to the internal pattern and delegates {@link LineMatcher} operations to it.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    protected final class MatcherAdapter extends LineMatcher {

        /**
         * The matcher.
         */
        private final Matcher matcher;

        /**
         * <p>
         * The constructor.
         * </p>
         *
         * @param line the line
         */
        private MatcherAdapter(final String line) {
            super(line);
            logger.debug("Matching {}", line);
            matcher = pattern.matcher(line);
        }

        /**
         * <p>
         * Returns the wrapped matcher.
         * </p>
         *
         * @return the matcher
         */
        public Matcher getMatcher() {
            return matcher;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean find() {
            return matcher.find();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int start() {
            return matcher.start();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int start(final int group) {
            return matcher.start(group);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int end() {
            return matcher.end();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int end(final int group) {
            return matcher.end(group);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String group() {
            return matcher.group();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String group(final int group) {
            return matcher.group(group);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int groupCount() {
            return matcher.groupCount();
        }
    }
}
