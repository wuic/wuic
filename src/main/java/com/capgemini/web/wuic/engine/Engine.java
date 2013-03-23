/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * •   The above copyright notice and this permission notice shall be included in
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

package com.capgemini.web.wuic.engine;

import com.capgemini.web.wuic.WuicResource;
import com.capgemini.web.wuic.configuration.Configuration;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * An engine is in charge to parse a set of files. They are generally able to
 * parse an unique kind of file {@link com.capgemini.web.wuic.FileType type}.
 * </p>
 * 
 * <p>
 * WUIC framework consists of a set of {@link Engine} to be executed. They are
 * structured using the chain of responsibility design pattern. Each engine is
 * in charge of the execution of the next engine and could decide not to execute
 * it.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.1.0
 */
public abstract class Engine {

    /**
     * The next engine.
     */
    protected Engine nextEngine;
    
    /**
     * <p>
     * Parses the given files and returns the result of this operation.
     * </p>
     * 
     * <p>
     * Should throw an {@link IllegalArgumentException} the files type is not
     * supported by this {@link Engine}.
     * </p>
     * 
     * @param files the files to parse
     * @return the parsed files
     * @throws IOException if any kind of I/O error occurs
     */
    public abstract List<WuicResource> parse(List<WuicResource> files)
            throws IOException;

    /**
     * <p>
     * Gets the {@link Configuration} used by this engine.
     * </p>
     * 
     * @return the configuration
     */
    public abstract Configuration getConfiguration();
    
    
    /**
     * <p>
     * Returns a flag indicating if the engine is configured to do something
     * when {@link Engine#parse(List)} is called or not.
     * </p>
     * 
     * @return {@code true} is something will be done, {@code false} otherwise
     */
    public abstract Boolean works();
    
    /**
     * <p>
     * The next {@link Engine} to be execute din the chain of responsibility. If
     * it is not set, then this {@link Engine} is the last one to be executed.
     * </p>
     * 
     * @param next the next {@link Engine}
     */
    public void setNext(final Engine next) {
        nextEngine = next;
    }
    
    /**
     * <p>
     * Returns the next engine previously set with {@link Engine#setNext(Engine)}
     * method.
     * </p>
     * 
     * @return the next {@link Engine}
     */
    public Engine getNext() {
        return nextEngine;
    }
}
