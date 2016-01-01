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

/**
 * <p>
 * All the error codes which identify the exceptions thrown by WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.4
 */
public interface ErrorCode {

    /**
     * An IO error has occurred. It is raised typically when a nut could not be read of written.
     */
    long IO_ERROR = 19860606001L;

    /**
     * An expected class could not be instantiated with an actual value.
     * This could be related to a {@code ClassNotFoundException} or any exception related to instantiation.
     */
    long UNABLE_TO_INSTANTIATE = 19860606002L;

    /**
     * An argument is not of an expected state or is null while it should.
     */
    long BAD_ARGUMENT_EXCEPTION = 19860606003L;

    /**
     * {@link com.github.wuic.nut.dao.NutDao#save(com.github.wuic.nut.Nut)} is not supported.
     */
    long SAVE_NOT_SUPPORTED_EXCEPTION = 19860606004L;

    /**
     * {@link com.github.wuic.nut.dao.NutDao} polling feature is not supported.
     */
    long POLLING_NOT_SUPPORTED_EXCEPTION = 19860606005L;

    /**
     * A state is not of an expected state or is null while it should.
     */
    long BAD_STATE_EXCEPTION = 19860606006L;

    /**
     * Default code when the wuic.xml path can't be read.
     */
    long XML_CANNOT_READ = 19860606100L;

    /**
     * No configuration ID has been defined for a configuration in the wuic.xml path.
     */
    long XML_NO_CONFIGURATION_ID = 19860606101L;

    /**
     * No id has been defined for a nut DAO builder in the wuic.xml path.
     */
    long XML_NO_NUT_DAO_BUILDER_ID = 19860606101L;

    /**
     * Not all required elements have been defined for a configuration in the wuic.xml path.
     */
    long XML_MISSING_CONFIGURATION_ELEMENT = 19860606103L;

    /**
     * No class has been defined for a nut dao builder in the wuic.xml path.
     */
    long XML_NO_NUT_DAO_BUILDER_CLASS = 19860606104L;

    /**
     * An expected class could not be instantiated with an actual value.
     * This could be related to a {@code ClassNotFoundException} or any exception related to instantiation.
     */
    long XML_UNABLE_TO_INSTANTIATE = 19860606105L;

    /**
     * A value defined in the wuic.xml is not convertible into a mandatory type.
     */
    long XML_BAD_TYPE_OF_VALUE = 19860606106L;

    /**
     * A reference is made to a non existing dao builder.
     */
    long XML_BAD_REF_TO_DAO_BUILDER = 19860606107L;

    /**
     * Indicates that a nut has not been found.
     */
    long NUT_NOT_FOUND = 19860606200L;

    /**
     * Indicates that a workflow has not been found.
     */
    long WORKFLOW_NOT_FOUND = 19860606201L;

    /**
     * Indicates that a property defined in {@link com.github.wuic.ApplicationConfig} is not supported by an
     * {@link com.github.wuic.config.AbstractObjectBuilder}.
     */
    long BUILDER_PROPERTY_NOT_SUPPORTED = 19860606203L;

    /**
     * Indicates that the {@link com.github.wuic.engine.core.StaticEngine} did not found a retrieved workflow.
     */
    long STATIC_WORKFLOW_NOT_FOUND = 19860606204L;

    /**
     * Indicates that a workflow template has not been found.
     */
    long WORKFLOW_TEMPLATE_NOT_FOUND = 19860606205L;

    /**
     * Indicates that a workflow doesn't define one and only one of its 'id' and 'idPrefix' attributes.
     */
    long WORKFLOW_IDENTIFIER = 19860606206L;

    /**
     * <p>
     * Gets the error code.
     * </p>
     *
     * @return the error code
     */
    long getErrorCode();

    /**
     * <p>
     * Gets the message related to the error code.
     * </p>
     *
     * @return the message
     */
    String getMessage();
}
