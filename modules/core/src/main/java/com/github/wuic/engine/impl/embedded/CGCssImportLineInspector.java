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


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.engine.LineInspector;
import com.github.wuic.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This class inspects CSS files to extract resources referenced with @import statement to process it. Then it adapts
 * the path of those processed resources to be accessible when exposed to the browser through WUIC uri
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.3
 */
public class CGCssImportLineInspector implements LineInspector {

    /**
     * Finds the URL within CSS script. The pattern describes a string :
     * <ul>
     * <li>starting with @import</li>
     * <li>followed by a space and eventually by 'url', a space and ('</li>
     * <li>followed by a double quote</li>
     * <li>followed by everything which does not contain a double quote (the file name)</li>
     * <li>followed by a double quote and eventually a space and a ')'</li>
     * <li>ending with a comma</li>
     * </ul>
     */
    private static final Pattern CSS_IMPORT_PATTERN = Pattern.compile("@import\\s(\\s?url\\s?\\()?\"([^\"]*)\"\\s?\\)?\\s?;");

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * {@inheritDoc}
     */
    @Override
    public Pattern getPattern() {
        return CSS_IMPORT_PATTERN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String appendTransformation(final Matcher matcher,
                                       final StringBuilder replacement,
                                       final int depth,
                                       final String resourceLocation) {

        final String referencedPath = matcher.group(NumberUtils.TWO);
        log.info("@import statement found for resource {}", referencedPath);

        // Extract the resource
        final String resourceName = resourceLocation + referencedPath;

        // Replace with the resource name
        replacement.append("@import url('");

        // Resolve relative path
        for (int i = 0; i < depth; i++) {
            replacement.append("../");
        }

        replacement.append(resourceName);
        replacement.append("');");

        return resourceName;
    }
}
