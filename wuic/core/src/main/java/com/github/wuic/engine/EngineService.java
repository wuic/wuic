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

import com.github.wuic.config.ServiceLoaderClasses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * This annotation marks a {@link com.github.wuic.engine.Engine} implementation that must be discovered
 * during classpath scanning.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ServiceLoaderClasses(Engine.class)
public @interface EngineService {

    /**
     * Default package to scan for this service.
     */
    String DEFAULT_SCAN_PACKAGE = "com.github.wuic.engine";

    /**
     * <p>
     * Indicates to the {@link com.github.wuic.context.ContextBuilder} that this service should injected or not to any new
     * default workflow.
     * </p>
     *
     * @return {@code true} if inject by default, {@code false} otherwise
     */
    boolean injectDefaultToWorkflow();

    /**
     * <p>
     * Indicates if this service provides a core engine.
     * Default value must not be changed by WUIC extension.
     * </p>
     *
     * @return {@code true} if this is a core engine, {@code false} otherwise
     */
    boolean isCoreEngine() default false;
}
