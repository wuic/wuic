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


package com.github.wuic.nut;

import com.github.wuic.NutType;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * <p>
 * The nut is the fundamental element of WUIC nut. It's represent a nut, generally corresponding to a static (CSS,
 * Javascript, Image). It could be picked thanks to different protocol (FTP, HTTP, cloud, etc).
 * </p>
 *
 * <p>
 * The name is the path relative to a location specified by a {@link com.github.wuic.nut.dao.NutDao} which is in charge of nut creations.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.1.1
 */
public interface Nut {

    /**
     * <p>
     * Opens and returns an {@code InputStream} pointing to the nut.
     * </p>
     *
     * @return the opened input stream
     * @throws IOException if an I/O error occurs
     */
    InputStream openStream() throws IOException;

    /**
     * <p>
     * Returns the path type of this nut.
     * </p>
     * 
     * @return the path type
     */
    NutType getInitialNutType();

    /**
     * <p>
     * Returns the name of the nut. This is not a path consequently it should not contains any paths.
     * </p>
     *
     * @return the nut name
     */
    String getInitialName();

    /**
     * <p>
     * Sets a proxy URI that allow to access the nut through another way.
     * </p>
     *
     * @param uri the proxy uri
     */
    void setProxyUri(String uri);

    /**
     * <p>
     * Gets the proxy URI.
     * </p>
     *
     * @return the proxy URI, {@code null} if no proxy URI is set
     */
    String getProxyUri();

    /**
     * <p>
     * Returns an {@code Long} indicating the version of this nut. This helps to deal with content updates.
     * </p>
     *
     * @return the nut's version
     */
    Future<Long> getVersionNumber();

    /**
     * <p>
     * If the file is stored in a directory on the file system, then this method returns the directory.
     * </p>
     *
     * @return the parent directory, {@code null} if the nut is not directly accessible on the file system
     */
    String getParentFile();

    /**
     * <p>
     * Indicates if this nut has a dynamic content, meaning that it could be changed each time {@link #openStream()} is
     * invoked. In this case the nut should not be added to any cache and transformation chain should be applied.
     * </p>
     *
     * @return {@code true} if content is dynamic, {@code false} otherwise
     */
    boolean isDynamic();
}
