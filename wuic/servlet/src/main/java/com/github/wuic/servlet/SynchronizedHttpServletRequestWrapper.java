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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.servlet.http.ProtocolHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * <p>
 * implementation that make possible a {@link HttpServletRequest} in a multi-threaded context.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.2
 */
public class SynchronizedHttpServletRequestWrapper extends HttpServletRequestWrapper {

    /**
     * Synchronized object.
     */
    private ServletRequest mutex;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param request the request to synchronize
     */
    public SynchronizedHttpServletRequestWrapper(final HttpServletRequest request) {
        super(request);

        mutex = request;

        while (mutex instanceof HttpServletRequestWrapper) {
            mutex = HttpServletRequestWrapper.class.cast(mutex).getRequest();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthType() {
        synchronized (mutex) {
            return super.getAuthType();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cookie[] getCookies() {
        synchronized (mutex) {
            return super.getCookies();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDateHeader(final String name) {
        synchronized (mutex) {
            return super.getDateHeader(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(final String name) {
        synchronized (mutex) {
            return super.getHeader(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getHeaders(final String name) {
        synchronized (mutex) {
            return super.getHeaders(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        synchronized (mutex) {
            return super.getHeaderNames();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIntHeader(final String name) {
        synchronized (mutex) {
            return super.getIntHeader(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMethod() {
        synchronized (mutex) {
            return super.getMethod();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPathInfo() {
        synchronized (mutex) {
            return super.getPathInfo();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPathTranslated() {
        synchronized (mutex) {
            return super.getPathTranslated();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContextPath() {
        synchronized (mutex) {
            return super.getContextPath();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueryString() {
        synchronized (mutex) {
            return super.getQueryString();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteUser() {
        synchronized (mutex) {
            return super.getRemoteUser();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUserInRole(final String role) {
        synchronized (mutex) {
            return super.isUserInRole(role);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Principal getUserPrincipal() {
        synchronized (mutex) {
            return super.getUserPrincipal();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestedSessionId() {
        synchronized (mutex) {
            return super.getRequestedSessionId();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestURI() {
        synchronized (mutex) {
            return super.getRequestURI();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringBuffer getRequestURL() {
        synchronized (mutex) {
            return super.getRequestURL();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletPath() {
        synchronized (mutex) {
            return super.getServletPath();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpSession getSession(final boolean create) {
        synchronized (mutex) {
            return super.getSession(create);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpSession getSession() {
        synchronized (mutex) {
            return super.getSession();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String changeSessionId() {
        synchronized (mutex) {
            return super.changeSessionId();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        synchronized (mutex) {
            return super.isRequestedSessionIdValid();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        synchronized (mutex) {
            return super.isRequestedSessionIdFromCookie();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        synchronized (mutex) {
            return super.isRequestedSessionIdFromURL();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        synchronized (mutex) {
            return super.isRequestedSessionIdFromUrl();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean authenticate(final HttpServletResponse response) throws IOException, ServletException {
        synchronized (mutex) {
            return super.authenticate(response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void login(final String username, final String password) throws ServletException {
        synchronized (mutex) {
            super.login(username, password);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logout() throws ServletException {
        synchronized (mutex) {
            super.logout();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        synchronized (mutex) {
            return super.getParts();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Part getPart(final String name) throws IOException, ServletException {
        synchronized (mutex) {
            return super.getPart(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upgrade(final ProtocolHandler handler) throws IOException {
        synchronized (mutex) {
            super.upgrade(handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletRequest getRequest() {
        return super.getRequest();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRequest(final ServletRequest request) {
        synchronized (mutex) {
            super.setRequest(request);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(final String name) {
        synchronized (mutex) {
            return super.getAttribute(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        synchronized (mutex) {
            return super.getAttributeNames();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCharacterEncoding() {
        synchronized (mutex) {
            return super.getCharacterEncoding();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCharacterEncoding(final String enc) throws UnsupportedEncodingException {
        synchronized (mutex) {
            super.setCharacterEncoding(enc);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getContentLength() {
        synchronized (mutex) {
            return super.getContentLength();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getContentLengthLong() {
        synchronized (mutex) {
            return super.getContentLengthLong();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {
        synchronized (mutex) {
            return super.getContentType();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        synchronized (mutex) {
            return super.getInputStream();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParameter(final String name) {
        synchronized (mutex) {
            return super.getParameter(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        synchronized (mutex) {
            return super.getParameterMap();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getParameterNames() {
        synchronized (mutex) {
            return super.getParameterNames();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getParameterValues(final String name) {
        synchronized (mutex) {
            return super.getParameterValues(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProtocol() {
        synchronized (mutex) {
            return super.getProtocol();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        synchronized (mutex) {
            return super.getScheme();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerName() {
        synchronized (mutex) {
            return super.getServerName();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getServerPort() {
        synchronized (mutex) {
            return super.getServerPort();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedReader getReader() throws IOException {
        synchronized (mutex) {
            return super.getReader();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteAddr() {
        synchronized (mutex) {
            return super.getRemoteAddr();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteHost() {
        synchronized (mutex) {
            return super.getRemoteHost();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final String name, final Object o) {
        synchronized (mutex) {
            super.setAttribute(name, o);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAttribute(final String name) {
        synchronized (mutex) {
            super.removeAttribute(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Locale getLocale() {
        synchronized (mutex) {
            return super.getLocale();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<Locale> getLocales() {
        synchronized (mutex) {
            return super.getLocales();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure() {
        synchronized (mutex) {
            return super.isSecure();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RequestDispatcher getRequestDispatcher(final String path) {
        synchronized (mutex) {
            return super.getRequestDispatcher(path);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRealPath(final String path) {
        synchronized (mutex) {
            return super.getRealPath(path);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRemotePort() {
        synchronized (mutex) {
            return super.getRemotePort();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalName() {
        synchronized (mutex) {
            return super.getLocalName();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalAddr() {
        synchronized (mutex) {
            return super.getLocalAddr();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLocalPort() {
        synchronized (mutex) {
            return super.getLocalPort();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletContext getServletContext() {
        synchronized (mutex) {
            return super.getServletContext();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext startAsync() {
        synchronized (mutex) {
            return super.startAsync();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) {
        synchronized (mutex) {
            return super.startAsync(servletRequest, servletResponse);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsyncStarted() {
        synchronized (mutex) {
            return super.isAsyncStarted();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsyncSupported() {
        synchronized (mutex) {
            return super.isAsyncSupported();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext getAsyncContext() {
        synchronized (mutex) {
            return super.getAsyncContext();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWrapperFor(final ServletRequest wrapped) {
        synchronized (mutex) {
            return super.isWrapperFor(wrapped);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWrapperFor(final Class<?> wrappedType) {
        synchronized (mutex) {
            return super.isWrapperFor(wrappedType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DispatcherType getDispatcherType() {
        synchronized (mutex) {
            return super.getDispatcherType();
        }
    }
}