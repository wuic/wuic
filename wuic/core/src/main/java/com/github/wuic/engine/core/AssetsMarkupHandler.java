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


package com.github.wuic.engine.core;

import java.util.Map;

/**
 * <p>
 * This class exposes in SAX-style way the ability to handle web assets (Javascript, CSS, images, ...),
 * comments and WUIC built-in components ({@code wuic:html-import}) when a HTML document is parsed.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public interface AssetsMarkupHandler {

    /**
     * <p>
     * Handles a comment with its content.
     * </p>
     *
     * @param content the comment content
     * @param startLine the starting line (first index is 1)
     * @param startColumn the starting column (first index is 1)
     * @param length the content length
     */
    void handleComment(char[] content, int startLine, int startColumn, int length);

    /**
     * <p>
     * Handles a javascript asset with its content.
     * </p>
     *
     * @param content the javascript content
     * @param attributes the attributes
     * @param startLine the starting line (first index is 1)
     * @param startColumn the starting column (first index is 1)
     * @param endLine the ending line
     * @param endColumn the ending column
     */
    void handleJavascriptContent(char[] content,
                                 Map<String, String> attributes,
                                 int startLine,
                                 int startColumn,
                                 int endLine,
                                 int endColumn);

    /**
     * <p>
     * Handles a script asset (like javascript) with its link.
     * </p>
     *
     * @param link the resource link
     * @param attributes the other attributes
     * @param startLine the starting line (first index is 1)
     * @param startColumn the starting column (first index is 1)
     * @param endLine the ending line
     * @param endColumn the ending column
     */
    void handleScriptLink(String link, Map<String, String> attributes, int startLine, int startColumn, int endLine, int endColumn);

    /**
     * <p>
     * Handles {@code <link>} that could refer resources like CSS or ICO.
     * </p>
     *
     * @param link the resource link
     * @param attributes the other attributes
     * @param startLine the starting line (first index is 1)
     * @param startColumn the starting column (first index is 1)
     * @param endLine the ending line
     * @param endColumn the ending column
     */
    void handleLink(String link,
                    Map<String, String> attributes,
                    int startLine,
                    int startColumn,
                    int endLine,
                    int endColumn);

    /**
     * <p>
     * Handles CSS asset with its content.
     * </p>
     *
     * @param content the CSS content
     * @param attributes the attributes
     * @param startLine the starting line (first index is 1)
     * @param startColumn the starting column (first index is 1)
     * @param endLine the ending line
     * @param endColumn the ending column
     */
    void handleCssContent(char[] content,
                          Map<String, String> attributes,
                          int startLine,
                          int startColumn,
                          int endLine,
                          int endColumn);

    /**
     * <p>
     * Handles image with its link.
     * </p>
     *
     * @param link the resource link
     * @param attributes the other attributes
     * @param startLine the starting line (first index is 1)
     * @param startColumn the starting column (first index is 1)
     * @param endLine the ending line
     * @param endColumn the ending column
     */
    void handleImgLink(String link,
                       Map<String, String> attributes,
                       int startLine,
                       int startColumn,
                       int endLine,
                       int endColumn);

    /**
     * <p>
     * Handles {@code html-import}.
     * </p>
     *
     * @param workflowId the imported workflow ID
     * @param attributes the other attributes
     * @param startLine the starting line (first index is 1)
     * @param startColumn the starting column (first index is 1)
     * @param endLine the ending line
     * @param endColumn the ending column
     */
    void handleImport(String workflowId,
                      Map<String, String> attributes,
                      int startLine,
                      int startColumn,
                      int endLine,
                      int endColumn);
}