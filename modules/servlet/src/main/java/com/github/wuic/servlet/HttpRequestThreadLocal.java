package com.github.wuic.servlet;

import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * <p>
 * Tool that can be used to bind to a {@link ThreadLocal} an HTTP request state to know if GZIP is supported and then
 * write according to this information a {@link Nut} to the HTTP response.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public enum HttpRequestThreadLocal implements Runnable {

    /**
     * Singleton.
     */
    INSTANCE;

    /**
     * Servlet request supporting GZIP or not during workflow processing.
     */
    private final ThreadLocal<Boolean> canGzipThreadLocal = new ThreadLocal<Boolean>();

    /**
     * The charset used to write the response.
     */
    private final String charset;

    /**
     * Builds a new instance.
     */
    private HttpRequestThreadLocal() {
        charset = System.getProperty("file.encoding");
    }

    /**
     * <p>
     * Indicates if the {@link javax.servlet.http.HttpServletRequest} bound to the current {@link #canGzipThreadLocal}
     * thread local supports GZIP.
     * </p>
     *
     * @return {@code true} if it supports GZIP or if not http request is bound, {@code false} otherwise
     */
    public boolean canGzip() {
        final Boolean canGzip = canGzipThreadLocal.get();
        return canGzip == null || canGzip;
    }

    /**
     * <p>
     * Indicates if the given {@link javax.servlet.http.HttpServletRequest} supports GZIP or not in
     * {@link #canGzipThreadLocal} thread local.
     * </p>
     */
    public Runnable canGzip(final HttpServletRequest request) {
        final Boolean can;

        if (request != null) {
            final String acceptEncoding = request.getHeader("Accept-Encoding");

            // Accept-Encoding must be set and GZIP specific
            can = acceptEncoding != null && acceptEncoding.contains("gzip");
        } else {
            can = Boolean.TRUE;
        }

        canGzipThreadLocal.set(can);
        return this;
    }

    /**
     * <p>
     * Serves the given nut by changing the specified response's state. The method sets headers and writes response.
     * </p>
     *
     * @param nut the nut to write
     * @param response the response
     * @throws NutNotFoundException if stream could not be opened
     * @throws StreamException if stream could not be written
     */
    public void write(final Nut nut, final HttpServletResponse response) throws NutNotFoundException, StreamException {
        response.setCharacterEncoding(charset);
        response.setContentType(nut.getNutType().getMimeType());

        // We set a far expiration date because we assume that polling will change the timestamp in path
        response.setHeader("Expires", "Sat, 06 Jun 2086 09:35:00 GMT");

        InputStream is = null;

        try {
            if (canGzip() && nut.isCompressed()) {
                response.setHeader("Content-Encoding", "gzip");
                response.setHeader("Vary", "Accept-Encoding");
                is = nut.openStream();
            } else {
                is = nut.isCompressed() ? new GZIPInputStream(nut.openStream()) : nut.openStream();
            }

            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copyStream(is, bos);
            response.setContentLength(bos.toByteArray().length);
            response.getOutputStream().write(bos.toByteArray());
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        } finally {
            IOUtils.close(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        canGzipThreadLocal.remove();
    }
}
