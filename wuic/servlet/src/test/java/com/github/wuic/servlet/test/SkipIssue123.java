package com.github.wuic.servlet.test;

import com.github.wuic.NutType;
import com.github.wuic.servlet.HtmlParserFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * <p>
 * Filter that helps testing {@link HtmlParserFilter} by skipping the issue 123.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.0
 */
public class SkipIssue123 extends HtmlParserFilter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        super.doFilter(request, new HttpServletResponseWrapper(HttpServletResponse.class.cast(response)) {

            /**
             * {@inheritDoc}
             */
            @Override
            public String getContentType() {
                return NutType.HTML.getMimeType();
            }
        }, chain);
    }
}
