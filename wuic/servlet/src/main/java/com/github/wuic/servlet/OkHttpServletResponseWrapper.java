package com.github.wuic.servlet;

import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This wrapper makes sure that the response will contain a non-empty body by removing caching headers that could lead
 * the server to respond with a {@link #SC_NOT_MODIFIED} status instead of a {@link #SC_OK} status.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
public class OkHttpServletResponseWrapper extends HttpServletResponseAdapter {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public OkHttpServletResponseWrapper() {
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param httpServletResponse a response to wrap
     */
    public OkHttpServletResponseWrapper(final HttpServletResponse httpServletResponse) {
        super(httpServletResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDateHeader(final String name, final long date) {
        // Check header before add it
        if (!skipHeader(name)) {
            super.setDateHeader(name, date);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIntHeader(final String name, final int value) {
        // Check header before add it
        if (!skipHeader(name)) {
            super.setIntHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHeader(final String name, String value) {
        // Check header before add it
        if (!skipHeader(name)) {
            super.addHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeader(final String name, String value) {
        // Check header before add it
        if (!skipHeader(name)) {
            super.setHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDateHeader(final String name, final long date) {
        // Check header before add it
        if (!skipHeader(name)) {
            super.addDateHeader(name, date);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addIntHeader(final String name, final int value) {
        // Check header before add it
        if (!skipHeader(name)) {
            super.addIntHeader(name, value);
        }
    }

    /**
     * <p>
     * Currently skips 'Last-Modified' and 'ETag' header to evict 304 (not modified) status. This WUIC, servlet filter
     * will always serve an up to date content.
     * </p>
     *
     * @param name the header name
     * @return {@code false} if header should be set, {@code true otherwise}
     */
    private boolean skipHeader(final String name) {
        // header to skip?
        return "last-modified".equalsIgnoreCase(name) || "etag".equalsIgnoreCase(name);
    }
}
