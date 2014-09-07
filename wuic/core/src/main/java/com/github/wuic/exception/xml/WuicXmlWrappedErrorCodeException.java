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
 * Simple {@link WuicXmlException} based on a delegated {@link com.github.wuic.exception.ErrorCode}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.4
 */
public class WuicXmlWrappedErrorCodeException extends WuicXmlException {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -2371436517844912089L;

    /**
     * The delegate error code.
     */
    private ErrorCode errorCode;

    /**
     * <p>
     * Builds a new exception based on a detailed message.
     * </p>
     *
     * @param ec the error code
     */
    public WuicXmlWrappedErrorCodeException(final ErrorCode ec) {
        super(ec.getMessage());
        errorCode = ec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getErrorCode() {
        return errorCode.getErrorCode();
    }
}
