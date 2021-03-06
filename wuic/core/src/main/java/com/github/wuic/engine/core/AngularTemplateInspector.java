/*
 * Copyright (c) 2016   The authors of WUIC
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


package com.github.wuic.engine.core;

import com.github.wuic.EnumNutType;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.RegexLineInspector;
import com.github.wuic.engine.ScriptLineInspector;
import com.github.wuic.engine.WithScriptLineInspector;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.PipedConvertibleNut;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.TimerTreeFactory;

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
 * @since 0.5.1
 */
@WithScriptLineInspector(condition = ScriptLineInspector.ScriptMatchCondition.NO_COMMENT)
public class AngularTemplateInspector extends RegexLineInspector {

    /**
     * Quote for 'templateUrl' match pattern.
     */
    private static final String QUOTE_TEMPLATE_URL = Pattern.quote("templateUrl");

    /**
     * Start of regex.
     */
    private static final String START_REGEX = String.format("%s'?\"?\\s*?\\:\\s*?", QUOTE_TEMPLATE_URL);

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param wrapPattern a regex describing a pattern wrapping the URL, could be {@code null}
     */
    public AngularTemplateInspector(final String wrapPattern) {
        super(Pattern.compile(START_REGEX + (wrapPattern == null ? STRING_LITERAL_WITH_TEMPLATE_REGEX : '(' + String.format(wrapPattern, "(.*?)") + ')'), Pattern.MULTILINE));
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
                                                             final CompositeNut.CompositeInput cis,
                                                             final ConvertibleNut originalNut)
            throws WuicException {
        final StringBuilder replacement = new StringBuilder();

        // group 2 when wrap pattern exists, group 1 otherwise
        final int groupIndex = matcher.groupCount() > 1 ? NumberUtils.TWO : 1;
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
    protected String toString(final TimerTreeFactory timerTreeFactory, final ConvertibleNut convertibleNut) throws IOException {
        if (convertibleNut.getNutType().isBasedOn(EnumNutType.HTML)) {
            return String.format("template:'%s'", NutUtils.readTransform(timerTreeFactory, convertibleNut)
                    .replace("'", "\\'")
                    .replace("\r\n", "'+'\\r\\n'+'")
                    .replace("\n", "'+'\\n'+'"));
        }

        return null;
    }
}
