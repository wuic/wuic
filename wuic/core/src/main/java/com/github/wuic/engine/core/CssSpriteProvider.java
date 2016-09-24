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
import com.github.wuic.NutTypeFactory;
import com.github.wuic.engine.Region;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Source;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.UrlProvider;

import java.io.IOException;

/**
 * <p>
 * This {@link com.github.wuic.engine.SpriteProvider} generates CSS code to represent the sprites.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.1
 */
public class CssSpriteProvider extends AbstractSpriteProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public ConvertibleNut getSprite(final String workflowId,
                                    final UrlProvider urlProvider,
                                    final String nutNameSuffix,
                                    final Source originals,
                                    final NutTypeFactory nutTypeFactory)
            throws IOException {
        final Long versionNumber = NutUtils.getVersionNumber(originals);
        final StringBuilder cssBuilder = new StringBuilder();

        // Define each region within the image
        for (final String name : getRegions().keySet()) {
            final Region reg = getRegions().get(name);
            final String className = convertAllowedName(workflowId, name);

            // declare class name
            cssBuilder.append(".");
            cssBuilder.append(className);

            // adds background URL
            cssBuilder.append("{display:inline-block;background:url('");
            cssBuilder.append(urlProvider.getUrl(getImage()));
            cssBuilder.append("') ");

            // x position
            cssBuilder.append(String.valueOf(reg.getxPosition() * -1));
            cssBuilder.append("px ");

            // y position
            cssBuilder.append(String.valueOf(reg.getyPosition() * -1));

            // width
            cssBuilder.append("px;width:");
            cssBuilder.append(String.valueOf((int) reg.getWidth()));

            // height
            cssBuilder.append("px;height:");
            cssBuilder.append(String.valueOf((int) reg.getHeight()));
            cssBuilder.append("px;}");
        }

        return new InMemoryNut(cssBuilder.toString().getBytes(),
                nutNameSuffix + "sprites.css",
                nutTypeFactory.getNutType(EnumNutType.CSS),
                originals,
                versionNumber);
    }
}
