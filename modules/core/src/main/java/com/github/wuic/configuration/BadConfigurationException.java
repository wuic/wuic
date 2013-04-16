////////////////////////////////////////////////////////////////////
//
// File: BadConfigurationException.java
// Created: 16 July 2012 11:00:00
// Author: GDROUET
// Copyright C 2012 Capgemini.
//
// All rights reserved.
//
////////////////////////////////////////////////////////////////////


package com.github.wuic.configuration;

/**
 * <p>
 * A 'BadConfigurationException' is thrown when a {@link Configuration} is not
 * properly defined according to the need of the class which uses it.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.1.0
 */
public class BadConfigurationException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -2371436517844912089L;
    
    /**
     * <p>
     * Default constructor.
     * </p>
     */
    public BadConfigurationException() {
        
    }

    /**
     * <p>
     * Builds an exception based on a detailed message.
     * </p>
     * 
     * @param message the detailed message
     */
    public BadConfigurationException(final String message) {
        super(message);
    }
    
    /**
     * <p>
     * Builds an exception based on the given {@code Throwable}.
     * </p>
     * 
     * @param throwable the {@code Throwable}
     */
    public BadConfigurationException(final Throwable throwable) {
        super(throwable);
    }
    
    /**
     * <p>
     * Builds an exception based on the given {@code Throwable}.
     * </p>
     * 
     * @param message the message
     * @param throwable the {@code Throwable}
     */
    public BadConfigurationException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
