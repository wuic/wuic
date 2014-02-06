/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.sample.thymeleaf;

import com.github.wuic.thymeleaf.WuicDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>
 * Filter that call thymeleaf when filtering the template.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.1
 */
public class ThymeleafFilter implements Filter {

    /**
     * Servlet context.
     */
    private ServletContext servletContext;

    /**
     * Template engine.
     */
    private TemplateEngine templateEngine;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(createTemplateResolver());
        templateEngine.setDialect(new WuicDialect());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        try {
            templateEngine.process("index", createContext(request, response), response.getWriter());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    /**
     * <p>
     * Creates the template resolver.
     * </p>
     *
     * @return the resolver
     */
    private TemplateResolver createTemplateResolver() {
        final ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver();
        templateResolver.setPrefix("/sample/scroller/");
        templateResolver.setSuffix(".html");
        return templateResolver;
    }

    /**
     * <p>
     * Sets some header in the response
     * </p>
     *
     * @param response the servlet response
     * @return the casted response into an {@code HttpServletResponse}
     */
    protected HttpServletResponse setHeaders(final ServletResponse response) {
        final HttpServletResponse res = (HttpServletResponse) response;
        res.setContentType("text/html;charset=UTF-8");
        res.setHeader("Pragma", "no-cache");
        res.setHeader("Cache-Control", "no-cache");
        res.setDateHeader("Expires", 0);
        return res;
    }

    /**
     * <p>
     * Creates a new context.
     * </p>
     *
     * @param request the request
     * @param response the response
     * @return the web context wrapping both request and response
     */
    protected WebContext createContext(final ServletRequest request, final ServletResponse response) {
        return new WebContext((HttpServletRequest) request, setHeaders(response), servletContext, request.getLocale());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
    }
}
