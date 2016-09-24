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


package com.github.wuic.engine.core;

import java.io.Reader;
import java.io.Serializable;

/**
 * <p>
 * This interface represent a basic parser that uses a {@link Reader} to read content and notifies a particular
 * {@link AssetsMarkupHandler}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public interface AssetsMarkupParser extends Serializable {

    /**
     * <p>
     * Reads the content provided through the given {@link Reader} and notifies the {@link AssetsMarkupHandler handler}
     * indicated in parameter when any asset is detected.
     * </p>
     *
     * @param reader the reader
     * @param handler the handler
     */
    void parse(Reader reader, AssetsMarkupHandler handler);
}
