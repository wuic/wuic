/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.exception;

import com.github.wuic.engine.core.StaticEngine;

import java.io.IOException;

/**
 * <p>
 * This exception is a global checked exception thrown by WUIC. This class also provides utility methods
 * that throw an appropriate exception for each requirement.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.4
 */
public class WuicException extends Exception {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -8457851407298538536L;

    /**
     * <p>
     * Builds a new {@link WuicException} with a detail message.
     * </p>
     *
     * @param message the detail message
     */
    WuicException(final String message) {
        super(message);
    }
    
    /**
     * <p>
     * Builds a new {@link WuicException} with an {@code Exception} to be wrapped.
     * </p>
     *
     * @param exception the wrapped {@code Exception}
     */
    WuicException(final Exception exception) {
        super(exception);
    }

    /**
     * <p>
     * Throws a new exception wrapping an origin.
     * </p>
     *
     * @param origin the origin
     * @throws WuicException the exception
     */
    public static void throwWuicException(final Exception origin) throws WuicException {
        throw new WuicException(origin);
    }

    /**
     * <p>
     * Throw an {@link IllegalArgumentException} indicating a builder property is not supported.
     * </p>
     *
     * @param key the property key
     * @param builderClass the built class
     */
    public static void throwBuilderPropertyNotSupportedException(final String key, final Class<?> builderClass) {
        throw new IllegalArgumentException(String.format("[Err n. %d] %s is not a property which is supported by the %s builder",
                ErrorCode.BUILDER_PROPERTY_NOT_SUPPORTED,
                key,
                builderClass.getName()));
    }

    /**
     *  <p>
     * Throws an {@link java.io.IOException} indicating that a a nut for a particular heap has not been found.
     * </p>
     *
     * @param nutName the name
     * @param heapId the heap
     * @throws java.io.IOException the exception
     */
    public static void throwNutNotFoundException(final String nutName, final String heapId) throws IOException {
        throw new NutNotFoundException(String.format("[Err n. %d] The nut with name '%s' has not been found in the heap identified with '%s'",
                ErrorCode.NUT_NOT_FOUND, nutName, heapId));
    }

    /**
     * <p>
     * Represents an exception which usually occurs when a IO issue is raised. This method throws an exception wrapping an
     * {@code IOException}.
     * </p>
     *
     * @param ioe the origin
     * @throws IOException the exception
     */
    public static void throwStreamException(final IOException ioe) throws IOException {
        throw new IOException(String.format("[Err n. %d] WUIC can't perform an I/O operation", ErrorCode.IO_ERROR), ioe);
    }

    /**
     * <p>
     * This method throws an {@link UnsupportedOperationException} to indicate that
     * {@link com.github.wuic.nut.dao.NutDao#save(com.github.wuic.nut.Nut)} os not supported by a given class.
     * </p>
     *
     * @param clazz the class
     */
    public static void throwSaveUnsupportedMethodException(final Class<?> clazz) {
        throw new UnsupportedOperationException(
                String.format("[Err n. %d] %s does not supports save(com.github.wuic.nut.Nut) method. It can only read nuts!",
                        ErrorCode.SAVE_NOT_SUPPORTED_EXCEPTION, clazz.getName()));
    }

    /**
     * <p>
     * This method throws an {@link IllegalArgumentException} when instantiation with reflection has failed.
     * </p>
     *
     * @param origin the origin
     */
    public static void throwUnableToInstantiateException(final Exception origin) {
        throw new IllegalArgumentException(String.format(
                "[Err n. %d] WUIC has failed to instantiate a class", ErrorCode.UNABLE_TO_INSTANTIATE), origin);
    }

    /**
     * <p>
     * This method throws a global {@link IllegalArgumentException}.
     * </p>
     *
     * @param origin the origin
     */
    public static void throwBadArgumentException(final Exception origin) {
        throw new IllegalArgumentException(String.format(
                "[Err n. %d] WUIC has detected an assertion violation with one argument", ErrorCode.BAD_ARGUMENT_EXCEPTION), origin);
    }

    /**
     * <p>
     * This method throws a global {@link IllegalStateException}.
     * </p>
     *
     * @param origin original exception
     */
    public static void throwBadStateException(final Exception origin) {
        throw new IllegalStateException(String.format(
                "[Err n. %d] WUIC has detected an assertion violation with one argument", ErrorCode.BAD_STATE_EXCEPTION), origin);
    }

   /**
    * <p>
    * Indicates that a workflow does not reference one and only one of both 'idPrefix' and 'id' attributes.
    * </p>
    *
    * @param id the ID
    * @param idPrefix the ID prefix
    */
    public static void throwWuicXmlWorkflowIdentifierException(final String idPrefix, final String id) {
        throw new IllegalArgumentException(
                String.format("[Err n. %d] The workflow must refer to one and only one of the attributes idPrefix (%s) and id (%s)",
                        ErrorCode.WORKFLOW_IDENTIFIER, idPrefix, id));
    }

    /**
     * <p>
     * Indicates that a wuic.xml file cannot be read.
     * </p>
     *
     * @param e the origin
     */
    public static void throwWuicXmlReadException(final Exception e) {
        throw new IllegalArgumentException(
                String.format("[Err n. %d] Unable to load wuic.xml", ErrorCode.XML_CANNOT_READ), e);
    }

    /**
     * <p>
     * Indicates that a wuic.json file cannot be read.
     * </p>
     *
     * @param e the origin
     */
    public static void throwWuicJsonReadException(final Exception e) {
        throw new IllegalArgumentException(
                String.format("[Err n. %d] Unable to load wuic.json", ErrorCode.JSON_CANNOT_READ), e);
    }

    /**
     * <p>
     * Throws an exception indicating that a workflow does not exists.
     * </p>
     *
     * @param workflowId the workflow ID
     * @throws WorkflowNotFoundException the exception
     */
    public static void throwWorkflowNotFoundException(final String workflowId) throws WorkflowNotFoundException {
        throw new WorkflowNotFoundException(
                String.format("[Err n. %d] The workflow identified with '%s' could not be found",
                        ErrorCode.WORKFLOW_NOT_FOUND, workflowId));
    }

    /**
     * <p>
     * Throws an exception indicating that a workflow template does not exists.
     * </p>
     *
     * @param workflowId the workflow ID
     * @throws WorkflowTemplateNotFoundException the exception
     */
    public static void throwWorkflowTemplateNotFoundException(final String workflowId) throws WorkflowTemplateNotFoundException {
        throw new WorkflowTemplateNotFoundException(
                String.format("[Err n. %d] Unable to find workflow template ID '%s'",
                        ErrorCode.WORKFLOW_TEMPLATE_NOT_FOUND, workflowId));
    }

    /**
     * <p>
     * Throws an exception indicating that a static workflow file does not exists.
     * </p>
     *
     * @param fileName the workflow file name
     * @throws StaticWorkflowNotFoundException the exception
     */
    public static void throwStaticWorkflowNotFoundException(final String fileName) throws StaticWorkflowNotFoundException {
        throw new StaticWorkflowNotFoundException(
                String.format("[Err n. %d] The engine '%s' did not found workflow '%s' in classpath",
                        ErrorCode.STATIC_WORKFLOW_NOT_FOUND, StaticEngine.class.getName(), fileName));
    }
}
