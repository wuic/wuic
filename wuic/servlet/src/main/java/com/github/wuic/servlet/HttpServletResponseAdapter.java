/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * <p>
 * An adapter for the {@link javax.servlet.http.HttpServletResponse} which applies a default behavior or delegate any
 * method call to a wrapped response if not {@code null}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 * @version 1.0
 */
public class HttpServletResponseAdapter implements HttpServletResponse {

    /**
     * A wrapped response.
     */
    private HttpServletResponse response;

    /**
     * Status is written here if wrapped {@link #response} is {@code null}.
     */
    private int status;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public HttpServletResponseAdapter() {
        status = SC_OK;
    }

    /**
     * <p>
     * Builds a new instance with a wrapped response.
     * </p>
     *
     * @param httpServletResponse the response
     */
    public HttpServletResponseAdapter(final HttpServletResponse httpServletResponse) {
        response = httpServletResponse;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCookie(final Cookie cookie) {
        // Delegate call
        if (response != null) {
            response.addCookie(cookie);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsHeader(final String name) {
        // Delegate call
        return response != null && response.containsHeader(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String encodeURL(final String url) {
        // Delegate call
        return (response != null) ? response.encodeURL(url) : url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String encodeRedirectURL(final String url) {
        // Delegate call
        return (response != null) ? response.encodeRedirectURL(url) : url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String encodeUrl(final String url) {
        // Delegate call
        return (response != null) ? response.encodeUrl(url) : url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String encodeRedirectUrl(final String url) {
        // Delegate call
        return (response != null) ? response.encodeRedirectUrl(url) : url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendError(final int sc, final String msg) throws IOException {
        // Delegate call
        if (response != null) {
            response.sendError(sc, msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendError(final int sc) throws IOException {
        // Delegate call
        if (response != null) {
            response.sendError(sc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendRedirect(final String location) throws IOException {
        // Delegate call
        if (response != null) {
            response.sendRedirect(location);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDateHeader(final String name, final long date) {
        // Delegate call
        if (response != null) {
            response.setDateHeader(name, date);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDateHeader(final String name, final long date) {
        // Delegate call
        if (response != null) {
            response.addDateHeader(name, date);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeader(final String name, final String value) {
        // Delegate call
        if (response != null) {
            response.setHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHeader(final String name, String value) {
        // Delegate call
        if (response != null) {
            response.addHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIntHeader(final String name, final int value) {
        // Delegate call
        if (response != null) {
            response.setIntHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addIntHeader(final String name, final int value) {
        // Delegate call
        if (response != null) {
            response.addIntHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatus(final int sc) {
        // Delegate call
        if (response != null) {
            response.setStatus(sc);
        } else {
            status = sc;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatus(final int sc, final String sm) {
        // Delegate call
        if (response != null) {
            response.setStatus(sc, sm);
        } else {
            status = sc;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getStatus() {
        // Delegate call
        return (response != null) ? response.getStatus() : status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(final String name) {
        // Delegate call
        return (response != null) ? response.getHeader(name) : name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getHeaders(final String name) {
        // Delegate call
        return (response != null) ? response.getHeaders(name) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getHeaderNames() {
        // Delegate call
        return (response != null) ? response.getHeaderNames() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCharacterEncoding() {
        // Delegate call
        return (response != null) ? response.getCharacterEncoding() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        // Delegate call
        return (response != null) ? response.getContentType() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        // Delegate call
        return (response != null) ? response.getOutputStream() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        // Delegate call
        return (response != null) ? response.getWriter() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCharacterEncoding(final String charset) {
        // Delegate call
        if (response != null) {
            response.setCharacterEncoding(charset);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContentLength(final int len) {
        // Delegate call
        if (response != null) {
            response.setContentLength(len);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContentLengthLong(final long len) {
        // Delegate call
        if (response != null) {
            response.setContentLengthLong(len);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContentType(final String type) {
        // Delegate call
        if (response != null) {
            response.setContentType(type);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBufferSize(final int size) {
        // Delegate call
        if (response != null) {
            response.setBufferSize(size);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBufferSize() {
        // Delegate call
        return (response != null) ? response.getBufferSize() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushBuffer() throws IOException {
        // Delegate call
        if (response != null) {
            response.flushBuffer();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetBuffer() {
        // Delegate call
        if (response != null) {
            response.resetBuffer();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCommitted() {
        // Delegate call
        return response != null && response.isCommitted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        // Delegate call
        if (response != null) {
            response.reset();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLocale(final Locale loc) {
        // Delegate call
        if (response != null) {
            response.setLocale(loc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locale getLocale() {
        // Delegate call
        return (response != null) ? response.getLocale() : null;
    }
}
