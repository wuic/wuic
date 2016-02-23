/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.RegexLineInspector;
import com.github.wuic.engine.ScriptLineInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * An AngularJS support that detects template URL.
 * </p>
 *
 * <p>
 * A template is not considered as a sub resource as angular loads it only when needed.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.1
 */
public class AngularTemplateInspector extends RegexLineInspector {

    /**
     * Quote for 'templateUrl' match pattern.
     */
    private static final String QUOTE_TEMPLATE_URL = Pattern.quote("templateUrl");

    /**
     * Start of regex.
     */
    private static final String START_REGEX =
            String.format("(/\\*(?:.)*?%s(?:.)*?\\*/)|(//(?:.)*?%s(?:.)*?\\n)|%s'?\"?\\s*?\\:\\s*?",
                    QUOTE_TEMPLATE_URL, QUOTE_TEMPLATE_URL, QUOTE_TEMPLATE_URL);

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param wrapPattern a regex describing a pattern wrapping the URL, could be {@code null}
     */
    public AngularTemplateInspector(final String wrapPattern) {
        super(Pattern.compile(START_REGEX + (wrapPattern == null ? STRING_LITERAL_REGEX : '(' + String.format(wrapPattern, "(.*?)") + ')'), Pattern.MULTILINE));
    }

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param wrapPattern a regex describing a pattern wrapping the URL, could be {@code null}
     * @return the new instance
     */
    public static LineInspector newInstance(final String wrapPattern) {
        return ScriptLineInspector.wrap(new AngularTemplateInspector(wrapPattern), ScriptLineInspector.ScriptMatchCondition.NO_COMMENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AppendedTransformation> appendTransformation(final LineMatcher matcher,
                                                             final EngineRequest request,
                                                             final CompositeNut.CompositeInputStream cis,
                                                             final ConvertibleNut originalNut)
            throws WuicException {
        final StringBuilder replacement = new StringBuilder();

        // group 4 when wrap pattern exists, group 3 otherwise
        final int groupIndex = matcher.groupCount() > NumberUtils.THREE ? NumberUtils.FOUR : NumberUtils.THREE;
        final String pathGroup = matcher.group(groupIndex);

        // Removes quotes
        String referencedPath = pathGroup;

        // Comment
        if (referencedPath == null) {
            replacement.append(matcher.group());
            return null;
        }

        referencedPath = referencedPath.substring(1, referencedPath.length() - 1);

        // Extract the nut
        final NutsHeap heap = getHeap(request, originalNut, cis, matcher.start(1));
        final List<Nut> nuts;

        try {
            nuts = heap.create(originalNut, referencedPath, NutDao.PathFormat.RELATIVE_FILE, request.getProcessContext());
        } catch (IOException ioe) {
            WuicException.throwWuicException(ioe);
            return null;
        }

        final List<? extends ConvertibleNut> res;
        final String group = matcher.group();
        final int pathGroupIndex = group.indexOf(pathGroup);
        replacement.append(group.substring(0, pathGroupIndex)).append('"');

        // URL is resolved successfully
        if (!nuts.isEmpty()) {
            res = manageAppend(new PipedConvertibleNut(nuts.iterator().next()), replacement, request, heap);

            // Set low download priority for templates
            for (final ConvertibleNut nut : res) {
                nut.setIsSubResource(false);
            }
        } else {
            // Can't resolve the URL but at least we can append a version number to it
            fallbackToVersionNumberInQueryString(replacement, referencedPath, originalNut);
            res = null;
        }

        replacement.append('"');
        replacement.append(group.substring(pathGroupIndex + pathGroup.length()));

        return Arrays.asList(new AppendedTransformation(matcher.start(), matcher.end(), res, replacement.toString()));
    }

    /**
     *{@inheritDoc}
     */
    @Override
    protected String toString(final ConvertibleNut convertibleNut) throws IOException {
        switch (convertibleNut.getNutType()) {
            case HTML:
                return String.format("template:'%s'", NutUtils.readTransform(convertibleNut)
                        .replace("'", "\\'")
                        .replace("\r\n", "'+'\\r\\n'+'")
                        .replace("\n", "'+'\\n'+'"));
            default:
                return null;
        }
    }
}
