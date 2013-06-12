////////////////////////////////////////////////////////////////////
//
// File: WuicXmlReadException.java
// Created: 16 July 2012 11:00:00
// Author: GDROUET
// Copyright C 2012 Capgemini.
//
// All rights reserved.
//
////////////////////////////////////////////////////////////////////


package com.github.wuic.exception.xml;

import com.github.wuic.exception.WuicException;

/**
 * <p>
 * Thrown when an error related the the wuic.xml file occurs.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.4
 */
public abstract class WuicXmlException extends WuicException {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -2371436517844912089L;

    /**
     * <p>
     * Builds a new exception based on a detailed message.
     * </p>
     *
     * @param message the message
     */
    public WuicXmlException(final String message) {
        super(message);
    }

    /**
     * <p>
     * Builds an exception based on the given {@code Exception}.
     * </p>
     *
     * @param message the message
     * @param ex the {@code Exception}
     */
    public WuicXmlException(final String message, final Exception ex) {
        super(message, ex);
    }
}
