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

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.servlet.http.ProtocolHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * An adapter for the {@link HttpServletRequest} which applies a default behavior or delegate any method call to a
 * wrapped request if not {@code null}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 * @version 1.0
 */
public class HttpServletRequestAdapter implements HttpServletRequest {

    /**
     * The wrapped request.
     */
    private final HttpServletRequest request;

    /**
     * Attributes map when request is null.
     */
    private final Map<String, Object> attributes;

    /**
     * A custom path info.
     */
    private final String pathInfo;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public HttpServletRequestAdapter() {
        // For test purpose only
        this(null, null);
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param pathInfo the path info
     */
    public HttpServletRequestAdapter(final String pathInfo) {
        this(null, pathInfo);
    }

    /**
     * <p>
     * Builds a new instance with a wrapped request.
     * </p>
     *
     * @param httpServletRequest the request
     */
    public HttpServletRequestAdapter(final HttpServletRequest httpServletRequest) {
        // For test purpose only
        this(httpServletRequest, null);
    }

    /**
     * <p>
     * Builds a new instance with a wrapped request and a specific path info.
     * </p>
     *
     * @param httpServletRequest the request
     * @param pi the path info
     */
    public HttpServletRequestAdapter(final HttpServletRequest httpServletRequest, final String pi) {
        request = httpServletRequest;
        attributes = request == null ? new HashMap<String, Object>() : null;
        pathInfo = pi;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthType() {
        // Delegate call
        return (request != null) ? request.getAuthType() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cookie[] getCookies() {
        // Delegate call
        return (request != null) ? request.getCookies() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDateHeader(final String name) {
        // Delegate call
        return (request != null) ? request.getDateHeader(name) : -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(final String name) {
        // Delegate call
        return (request != null) ? request.getHeader(name) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getHeaders(final String name) {
        // Delegate call
        return (request != null) ? request.getHeaders(name) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        // Delegate call
        return (request != null) ? request.getHeaderNames() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIntHeader(final String name) {
        // Delegate call
        return (request != null) ? request.getIntHeader(name) : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMethod() {
        // Delegate call
        return (request != null) ? request.getMethod() : "GET";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPathInfo() {
        // Delegate call
        return (request != null) ? request.getPathInfo() : pathInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPathTranslated() {
        // Delegate call
        return (request != null) ? request.getPathTranslated() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContextPath() {
        // Delegate call
        return (request != null) ? request.getContextPath() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueryString() {
        // Delegate call
        return (request != null) ? request.getQueryString() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteUser() {
        // Delegate call
        return (request != null) ? request.getRemoteUser() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUserInRole(final String role) {
        // Delegate call
        return (request != null) && request.isUserInRole(role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getUserPrincipal() {
        // Delegate call
        return (request != null) ? request.getUserPrincipal() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestedSessionId() {
        // Delegate call
        return (request != null) ? request.getRequestedSessionId() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestURI() {
        // Delegate call
        return (request != null) ? request.getRequestURI() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringBuffer getRequestURL() {
        // Delegate call
        return (request != null) ? request.getRequestURL() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletPath() {
        // Delegate call
        return (request != null) ? request.getServletPath() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpSession getSession(boolean create) {
        // Delegate call
        return (request != null) ? request.getSession(create) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpSession getSession() {
        // Delegate call
        return (request != null) ? request.getSession() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String changeSessionId() {
        // Delegate call
        return (request != null) ? request.changeSessionId() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        // Delegate call
        return (request != null) && request.isRequestedSessionIdValid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        // Delegate call
        return (request != null) && request.isRequestedSessionIdFromCookie();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        // Delegate call
        return (request != null) && request.isRequestedSessionIdFromURL();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        // Delegate call
        return (request != null) && request.isRequestedSessionIdFromUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(final HttpServletResponse response) throws IOException, ServletException {
        // Delegate call
        return (request != null) && request.authenticate(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(final String username, final String password) throws ServletException {
        // Delegate call
        if (request != null) {
            request.login(username, password);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout() throws ServletException {
        // Delegate call
        if (request != null) {
            request.logout();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        // Delegate call
        return (request != null) ? request.getParts() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Part getPart(final String name) throws IOException, ServletException {
        // Delegate call
        return (request != null) ? request.getPart(name) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upgrade(final ProtocolHandler handler) throws IOException {
        // Delegate call
        if (request != null) {
            request.upgrade(handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(final String name) {
        // Delegate call
        return (request != null) ? request.getAttribute(name) : attributes.get(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        // Delegate call
        return (request != null) ? request.getAttributeNames() : Collections.enumeration(attributes.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCharacterEncoding() {
        // Delegate call
        return (request != null) ? request.getCharacterEncoding() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCharacterEncoding(final String env) throws UnsupportedEncodingException {
        // Delegate call
        if (request != null) {
            request.setCharacterEncoding(env);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getContentLength() {
        // Delegate call
        return (request != null) ? request.getContentLength() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getContentLengthLong() {
        // Delegate call
        return (request != null) ? request.getContentLengthLong() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        // Delegate call
        return (request != null) ? request.getContentType() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        // Delegate call
        return (request != null) ? request.getInputStream() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParameter(final String name) {
        // Delegate call
        return (request != null) ? request.getParameter(name) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getParameterNames() {
        // Delegate call
        return (request != null) ? request.getParameterNames() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getParameterValues(final String name) {
        // Delegate call
        return (request != null) ? request.getParameterValues(name) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        // Delegate call
        return (request != null) ? request.getParameterMap() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProtocol() {
        // Delegate call
        return (request != null) ? request.getProtocol() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        // Delegate call
        return (request != null) ? request.getScheme() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerName() {
        // Delegate call
        return (request != null) ? request.getServerName() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getServerPort() {
        // Delegate call
        return (request != null) ? request.getServerPort() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedReader getReader() throws IOException {
        // Delegate call
        return (request != null) ? request.getReader() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteAddr() {
        // Delegate call
        return (request != null) ? request.getRemoteAddr() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteHost() {
        // Delegate call
        return (request != null) ? request.getRemoteHost() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final String name, final Object o) {
        // Delegate call
        if (request != null) {
            request.setAttribute(name, o);
        } else {
            attributes.put(name, o);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAttribute(final String name) {
        // Delegate call
        if (request != null) {
            request.removeAttribute(name);
        } else {
            attributes.remove(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locale getLocale() {
        // Delegate call
        return (request != null) ? request.getLocale() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<Locale> getLocales() {
        // Delegate call
        return (request != null) ? request.getLocales() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure() {
        // Delegate call
        return (request != null) && request.isSecure();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RequestDispatcher getRequestDispatcher(final String path) {
        // Delegate call
        return (request != null) ? request.getRequestDispatcher(path) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRealPath(final String path) {
        // Delegate call
        return (request != null) ? request.getRealPath(path) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRemotePort() {
        // Delegate call
        return (request != null) ? request.getRemotePort() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalName() {
        // Delegate call
        return (request != null) ? request.getLocalName() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalAddr() {
        // Delegate call
        return (request != null) ? request.getLocalAddr() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLocalPort() {
        // Delegate call
        return (request != null) ? request.getLocalPort() : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletContext getServletContext() {
        // Delegate call
        return (request != null) ? request.getServletContext() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext startAsync() {
        // Delegate call
        return (request != null) ? request.startAsync() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) {
        // Delegate call
        return (request != null) ? request.startAsync(servletRequest, servletResponse) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsyncStarted() {
        // Delegate call
        return (request != null) && request.isAsyncStarted();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsyncSupported() {
        // Delegate call
        return  (request != null) && request.isAsyncSupported();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext getAsyncContext() {
        // Delegate call
        return (request != null) ? request.getAsyncContext() : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DispatcherType getDispatcherType() {
        // Delegate call
        return (request != null) ? request.getDispatcherType() : null;
    }
}
