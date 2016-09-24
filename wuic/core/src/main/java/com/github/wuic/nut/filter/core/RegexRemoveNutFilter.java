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


package com.github.wuic.nut.filter.core;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.config.Alias;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.Config;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.nut.filter.NutFilterService;
import com.github.wuic.nut.filter.setter.RegexExpressionsPropertySetter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * This filter can remove paths matching a regex.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.5
 */
@NutFilterService
@Alias("regex")
public class RegexRemoveNutFilter implements NutFilter {

    /**
     * The compiled regex.
     */
    private List<Pattern> patterns;

    /**
     * If this filter is enabled.
     */
    private Boolean enable;

    /**
     * <p>
     * Initializes a new instance. If a regex can't be compiled, a {@link java.util.regex.PatternSyntaxException} will be thrown.
     * </p>
     *
     * @param enabled if the filter is activated
     * @param regex all the exclusion regex
     */
    @Config
    public void init(
            @BooleanConfigParam(propertyKey = ApplicationConfig.ENABLE,
                    defaultValue = true)
            final Boolean enabled,
            @ObjectConfigParam(propertyKey = ApplicationConfig.REGEX_EXPRESSIONS,
                    defaultValue = "",
                    setter = RegexExpressionsPropertySetter.class)
            final String[] regex) {
        patterns = new ArrayList<Pattern>();

        for (final String r : regex) {
            patterns.add(Pattern.compile(r));
        }

        enable = enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> filterPaths(final List<String> paths) {

        // Disabled: do nothing
        if (!enable) {
            return paths;
        }

        final List<String> retval = new ArrayList<String>();

        pathsLoop:
        for (final String path : paths) {
            for (final Pattern pattern : patterns) {

                // Won't keep the path in returned list if it matches one of the pattern
                if (pattern.matcher(path).matches()) {
                    continue pathsLoop;
                }
            }

            retval.add(path);
        }

        return retval;
    }
}
