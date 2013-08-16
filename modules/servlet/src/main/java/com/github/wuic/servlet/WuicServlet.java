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

import com.github.wuic.WuicFacade;
import com.github.wuic.exception.ErrorCode;
import com.github.wuic.exception.NutNotFoundException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
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
 * @version 1.6
 * @since 0.1.1
 */
public class WuicServlet extends HttpServlet {

    /**
     * WUIC's facade attribute name.
     */
    public static final String WUIC_FACADE_ATTRIBUTE = "WUIC_FACADE";

    /**
     * Init parameter which indicates the WUIC context path.
     */
    public static final String WUIC_SERVLET_CONTEXT_PARAM = "c.g.w.wuicContextPath";

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -7678202861072625737L;

    /**
     * The servlet context detected when this servlet was initialized.
     */
    private static ServletContext servletContext;
    
    /**
     * The servlet mapping.
     */
    private static String servletMapping;

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
        servletContext = config.getServletContext();
        servletMapping = config.getInitParameter(WUIC_SERVLET_CONTEXT_PARAM);

        // Context where resources will be exposed
        final String wuicCp = IOUtils.mergePath("/", servletContext().getContextPath(), servletMapping());

        log.info("WUIC's full context path is {}", wuicCp);

        try {
            final WuicFacade facade = WuicFacade.newInstance(wuicCp);
            config.getServletContext().setAttribute(WUIC_FACADE_ATTRIBUTE, facade);
        } catch (WuicException we) {
            throw new ServletException("Unable to initialize WuicServlet", we);
        }

        // Build expected URL pattern
        final StringBuilder patternBuilder = new StringBuilder();

        // Starts with servlet mapping
        patternBuilder.append(Pattern.quote(servletMapping()));

        if (!servletMapping().endsWith("/")) {
            patternBuilder.append("/");
        }

        // Followed by the group ID a slash and finally the page name
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
            response.getWriter().println("URL pattern. Expected [groupId]/[resourceName]");
            response.setStatus(HttpURLConnection.HTTP_BAD_REQUEST);
        } else {
            try {
                writeResource(matcher.group(1), matcher.group(NumberUtils.TWO), response);
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
     * @param groupId the group ID
     * @param resourceName the nut name
     * @param response the response
     * @throws WuicException if an I/O error occurs or nut not found
     */
    private void writeResource(final String groupId, final String resourceName, final HttpServletResponse response)
            throws WuicException {

        // Get the files group
        final List<Nut> files = getWuicFacade().getGroup(groupId);
        final Nut resource = getResource(files, resourceName);
        InputStream is = null;

        // Resource found
        if (resource != null) {
            try {
                response.setContentType(resource.getNutType().getMimeType());
                is = resource.openStream();
                IOUtils.copyStream(is, response.getOutputStream());
                is = null;
            } catch (IOException ioe) {
                throw new StreamException(ioe);
            } finally {
                IOUtils.close(is);
            }
        } else {
            throw new NutNotFoundException(resourceName, groupId);
        }
    }

    /**
     * <p>
     * Gets the nut with the specified name
     * </p>
     *
     * @param resources the resources where the research
     * @param resourceName the name of the nut
     * @return the nut corresponding to the name, {@code null} if nothing match
     */
    private Nut getResource(final List<Nut> resources, final String resourceName) {
        // Iterates the resources to find the requested element
        for (Nut resource : resources) {
            // Resource found : write the stream and return
            if (resource.getName().equals(resourceName) || ("/" + resource.getName()).equals(resourceName)) {
                return resource;
            } else if (resource.getReferencedNuts() != null) {
                // Find in referenced resources
                final Nut ref = getResource(resource.getReferencedNuts(), resourceName);

                if (ref != null) {
                    return ref;
                }
            }
        }

        return null;
    }

    /**
     * <p>
     * Gets the WUIC's facade from the servlet context.
     * </p>
     *
     * @return the WUIC's facade
     */
    public WuicFacade getWuicFacade() {
        return (WuicFacade) getServletContext().getAttribute(WUIC_FACADE_ATTRIBUTE);
    }

    /**
     * <p>
     * Returns the {@code ServletContext}.
     * </p>
     * 
     * @return the servlet context
     */
    public static ServletContext servletContext() {
        return servletContext;
    }
    
    /**
     * <p>
     * Returns the first servlet mapping for this servlet.
     * </p>
     * 
     * @return the servlet mapping
     */
    public static String servletMapping() {
        return servletMapping;
    }
}
