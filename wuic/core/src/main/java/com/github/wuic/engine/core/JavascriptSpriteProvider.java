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
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.engine.Region;
import com.github.wuic.nut.Source;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.UrlProvider;

import java.io.IOException;

/**
 * <p>
 * This class provides sprite throughout javascript language. The javascript path
 * defines an array which associates each aggregated image name to its region in
 * the final image.
 * </p>
 *
 * <p>
 * In Javascript, an object is declared in variable starting with WUIC_SPRITE_
 * and ending with the heap ID. The heap ID is treated to be in upper case and
 * with its characters which are invalid in javascript variable names replaced
 * with an underscore.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.2.0
 */
public class JavascriptSpriteProvider extends AbstractSpriteProvider {

    /**
     * Constant name.
     */
    private static final String JS_CONSTANT = "WUIC_SPRITE";

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
        final StringBuilder jsBuilder = new StringBuilder();

        // Inject instantiation
        jsBuilder.append("if (typeof(");
        jsBuilder.append(JS_CONSTANT);
        jsBuilder.append(") === 'undefined') {");
        jsBuilder.append(IOUtils.NEW_LINE);
        jsBuilder.append("\tvar ");
        jsBuilder.append(JS_CONSTANT);
        jsBuilder.append(" = {};");
        jsBuilder.append(IOUtils.NEW_LINE);
        jsBuilder.append("}");
        jsBuilder.append(IOUtils.NEW_LINE);

        // Define each region within the image
        for (final String name : getRegions().keySet()) {
            final Region reg = getRegions().get(name);
            
            // Instruction that affect the new object to the WUIC_SPRITE constant
            jsBuilder.append(JS_CONSTANT);
            jsBuilder.append("['");
            jsBuilder.append(convertAllowedName(workflowId, name));

            // x position
            jsBuilder.append("'] = {");
            jsBuilder.append(IOUtils.NEW_LINE);
            jsBuilder.append("\tx : \"");
            jsBuilder.append(reg.getxPosition());

            // y position
            jsBuilder.append("\",");
            jsBuilder.append(IOUtils.NEW_LINE);
            jsBuilder.append("\ty : \"");
            jsBuilder.append(reg.getyPosition());

            // Width
            jsBuilder.append("\",");
            jsBuilder.append(IOUtils.NEW_LINE);
            jsBuilder.append("\tw : \"");
            jsBuilder.append(reg.getWidth());

            // Height
            jsBuilder.append("\",");
            jsBuilder.append(IOUtils.NEW_LINE);
            jsBuilder.append("\th : \"");
            jsBuilder.append(reg.getHeight());

            // Add URL
            jsBuilder.append("\",");
            jsBuilder.append(IOUtils.NEW_LINE);
            jsBuilder.append("\turl : \"");
            jsBuilder.append(urlProvider.getUrl(getImage()));
            jsBuilder.append("\"");
            jsBuilder.append(IOUtils.NEW_LINE);
            jsBuilder.append("};");
            jsBuilder.append(IOUtils.NEW_LINE);
        }

        // Make a byte array and return nut wrapper
        return new InMemoryNut(jsBuilder.toString().getBytes(),
                nutNameSuffix + "sprites.js",
                nutTypeFactory.getNutType(EnumNutType.JAVASCRIPT),
                originals,
                versionNumber);
    }
}