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


package com.github.wuic.config;

import java.lang.annotation.*;

/**
 * <p>
 * This annotation allows to indicate on a service annotation (typically {@link com.github.wuic.engine.EngineService},
 * {@link com.github.wuic.nut.dao.NutDaoService}, {@link com.github.wuic.nut.filter.NutFilterService}) what are the abstract
 * classes and interfaces implementation that are annotated with it and can be loaded through the {@code ServiceLoader}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServiceLoaderClasses {

    /**
     * <p>
     * The abstract classes and interfaces associated to the implementation that should loaded through {@code ServiceLoader}.
     * </p>
     *
     * @return the abstract classes and interfaces to be loaded
     */
    Class[] value();
}
