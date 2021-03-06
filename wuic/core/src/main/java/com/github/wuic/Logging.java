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


package com.github.wuic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This is an enumeration of object wrapping special loggers. Each logger will use a TRACE level and could be configured
 * separately.
 * </p>
 *
 * <p>
 * A special logger should never be used to report an error or a warning.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.1
 */
public enum Logging {

    /**
     * Polling logger. Name is, 'com.github.wuic.Logging.POLL', you can see its logs by assigning the TRACE level.
     */
    POLL,

    /**
     * Timer logger. Name is, 'com.github.wuic.Logging.TIMER', you can see its logs by assigning the TRACE level.
     */
    TIMER,

    /**
     * Properties logger. Name is, 'com.github.wuic.Logging.PROPERTIES', you can see its logs by assigning the TRACE level.
     */
    PROPERTIES;

    /**
     * The special logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass().getName() + "." + name());

    /**
     * <p>
     * Logs in TRACE level.
     * </p>
     *
     * @param message the message
     * @param args the arguments
     */
    public void log(final String message, final Object ... args) {
        logger.trace(message, args);
    }
}
