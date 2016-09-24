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


package com.github.wuic.mbean;

import com.github.wuic.util.Input;

/**
 * <p>
 * Statistics object for a particular transformation.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class TransformationStat implements TransformationStatMXBean {

    /**
     * A {@code String} describing the data exchange.
     */
    private final String exchange;

    /**
     * The duration of the transformation.
     */
    private final long duration;

    /**
     * A description of this transformation.
     */
    private final String description;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param written {@code true} if the input has been read by the transformer, {@code false} otherwise
     * @param is the input used by transformer
     * @param duration the duration of transformation
     * @param description the description of this transformation
     */
    public TransformationStat(final boolean written,
                              final Input is,
                              final long duration,
                              final String description) {
        this.description = description;
        this.duration = duration;
        this.exchange = written ? String.format("Read %s from a source in %s", is.isReadAsByte()
                ? "bytes" : "chars", is.isSourceAsByte() ? "bytes" : "chars") : "no transformation applied";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExchange() {
        return exchange;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDuration() {
        return duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return description;
    }
}
