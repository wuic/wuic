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


package com.github.wuic.test.config;

import com.github.wuic.config.Config;
import com.github.wuic.config.IntegerConfigParam;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bad configured and annotated service.
 *
 * @author Guillaume DROUET
 * @since 0.5
 */
@IService
public class MyBadService implements I {

    int i = 0;

    /**
     * Some unsupported config annotation.
     */
    @Target({ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface UnsupportedAnnotation {

    }

    /**
     * Constructor without config annotation.
     *
     * @param i not annotated parameter
     */
    @Config
    public void initMyBadService(final int i) {
        this.i++;
    }

    /**
     * Constructor with an unsupported config annotation.
     *
     * @param s the parameter annotated with unsupported annotation
     */
    @Config
    public void initMyBadService(@UnsupportedAnnotation final String s) {
        i++;
    }

    /**
     * Constructor with one parameter not annotated.
     *
     * @param i not annotated
     * @param j annotated
     */
    @Config
    public void initMyBadService(final int i,
                        @IntegerConfigParam(defaultValue = 1, propertyKey = "foo") final int j) {
        this.i++;
    }
}
