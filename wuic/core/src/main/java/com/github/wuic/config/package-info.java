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

/**
 * <p>
 * This package contains all the API elements to make objects configurable. A configurable object provides
 * a constructor annotated with {@link com.github.wuic.config.Config}. Each parameter of this
 * constructor is annotated with a configuration parameter annotation:
 * <ul>
 *     <li>{@link com.github.wuic.config.ObjectConfigParam}</li>
 *     <li>{@link com.github.wuic.config.BooleanConfigParam}</li>
 *     <li>{@link com.github.wuic.config.StringConfigParam}</li>
 *     <li>{@link com.github.wuic.config.IntegerConfigParam}</li>
 * </ul>
 * </p>
 *
 * The object could be discovered over the classpath with an {@link com.github.wuic.config.ObjectBuilderFactory}.
 *
 * @author Guillaume DROUET
 */
package com.github.wuic.config;