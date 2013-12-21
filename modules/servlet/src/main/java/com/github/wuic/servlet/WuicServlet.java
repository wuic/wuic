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

import com.github.wuic.exception.ErrorCode;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.jee.WuicJeeContext;
import com.github.wuic.jee.WuicServletContextListener;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Abstract servlet which initializes the servlet context and the servlet mapping
 * used for WUIC.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.7
 * @since 0.1.1
 */
public class WuicServlet extends HttpServlet {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -7678202861072625737L;

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The expected pattern in request URI's.
     */
    private Pattern urlPattern;

    /**
     * Mapping error code to their HTTP code.
     */
    private Map<Long, Integer> errorCodeToHttpCode;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public WuicServlet() {
        errorCodeToHttpCode = new HashMap<Long, Integer>();

        errorCodeToHttpCode.put(ErrorCode.NUT_NOT_FOUND, HttpURLConnection.HTTP_NOT_FOUND);
        errorCodeToHttpCode.put(ErrorCode.WORKFLOW_NOT_FOUND, HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final ServletConfig config) throws ServletException {
        // Build expected URL pattern
        final StringBuilder patternBuilder = new StringBuilder();
        final String key = WuicServletContextListener.WUIC_SERVLET_CONTEXT_PARAM;
        final String servletMapping = config.getServletContext().getInitParameter(key);

        if (servletMapping == null) {
            throw new BadArgumentException(new IllegalArgumentException(String.format("Init param '%s' must be defined", key)));
        }

        // Starts with servlet mapping
        patternBuilder.append(Pattern.quote(servletMapping));

        if (!servletMapping.endsWith("/")) {
            patternBuilder.append("/");
        }

        // Followed by the workflow ID a slash and finally the page name
        patternBuilder.append("([^/]*)/(.*)");

        urlPattern = Pattern.compile(patternBuilder.toString());

        super.init(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        final Matcher matcher = urlPattern.matcher(request.getRequestURI());

        if (!matcher.find() || matcher.groupCount() != NumberUtils.TWO) {
            response.getWriter().println("URL pattern. Expected [workflowId]/[nutName]");
            response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
        } else {
            try {
                writeNut(matcher.group(1), matcher.group(NumberUtils.TWO), response);
            } catch (WuicException we) {
                log.error("Unable to retrieve nut", we);

                // Use 500 has default status code
                final Integer httpCode = errorCodeToHttpCode.containsKey(we.getErrorCode()) ?
                        errorCodeToHttpCode.get(we.getErrorCode()) : HttpURLConnection.HTTP_INTERNAL_ERROR;

                response.getWriter().println(we.getMessage());
                response.setStatus(httpCode);
            }
        }
    }

    /**
     * <p>
     * Writes a nut in the HTTP response.
     * </p>
     *
     * @param workflowId the workflow ID
     * @param nutName the nut name
     * @param response the response
     * @throws WuicException if an I/O error occurs or nut not found
     */
    private void writeNut(final String workflowId, final String nutName, final HttpServletResponse response)
            throws WuicException {

        // Get the nuts workflow
        final List<Nut> nuts = WuicJeeContext.getWuicFacade().runWorkflow(workflowId);
        final Nut nut = getNut(nuts, nutName);
        InputStream is = null;

        // Nut found
        if (nut != null) {
            try {
                response.setContentType(nut.getNutType().getMimeType());
                is = nut.openStream();
                IOUtils.copyStream(is, response.getOutputStream());
                is = null;
            } catch (IOException ioe) {
                throw new StreamException(ioe);
            } finally {
                IOUtils.close(is);
            }
        } else {
            throw new NutNotFoundException(nutName, workflowId);
        }
    }

    /**
     * <p>
     * Gets the nut with the specified name
     * </p>
     *
     * @param nuts the nuts where the research
     * @param nutName the name of the nut
     * @return the nut corresponding to the name, {@code null} if nothing match
     */
    private Nut getNut(final List<Nut> nuts, final String nutName) {
        // Iterates the nuts to find the requested element
        for (Nut nut : nuts) {
            // Nut found : write the stream and return
            if (nut.getName().equals(nutName) || ("/" + nut.getName()).equals(nutName)) {
                return nut;
            } else if (nut.getReferencedNuts() != null) {
                // Find in referenced nuts
                final Nut ref = getNut(nut.getReferencedNuts(), nutName);

                if (ref != null) {
                    return ref;
                }
            }
        }

        return null;
    }
}
