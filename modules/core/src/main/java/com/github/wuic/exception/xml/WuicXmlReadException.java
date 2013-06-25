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

import com.github.wuic.exception.ErrorCode;

/**
 * <p>
 * A 'WuicXmlReadException' is a default exception thrown when a wuic.xml path could not be read properly.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.1.0
 */
public class WuicXmlReadException extends WuicXmlException {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -2371436517844912089L;
    
    /**
     * <p>
     * Builds an exception based on the given {@code Exception}.
     * </p>
     * 
     * @param exception the {@code Exception}
     */
    public WuicXmlReadException(final Exception exception) {
        super("Unable to read wuic.xml file", exception);
    }
    
    /**
     * <p>
     * Builds an exception based on the given {@code Exception} and a detail message.
     * </p>
     * 
     * @param message the message
     * @param exception the {@code Exception}
     */
    public WuicXmlReadException(final String message, final Exception exception) {
        super(message, exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getErrorCode() {
        return ErrorCode.XML_CANNOT_READ;
    }
}
