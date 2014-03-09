/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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

import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.core.ByteArrayNut;
import com.github.wuic.NutType;
import com.github.wuic.nut.Nut;
import com.github.wuic.engine.Region;
import com.github.wuic.util.IOUtils;

import java.util.List;

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
 * @version 1.7
 * @since 0.2.0
 */
public class CGJavascriptSpriteProvider extends CGAbstractSpriteProvider {

    /**
     * Constant name.
     */
    private static final String JS_CONSTANT = "WUIC_SPRITE";

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut getSprite(final String url, final String heapId, final String nutNameSuffix, final List<Nut> originals)
            throws StreamException {
        final ByteArrayNut retval = new ByteArrayNut(nutNameSuffix + "sprites.js", NutType.JAVASCRIPT, originals);
        final StringBuilder jsBuilder = new StringBuilder();

        // Inject instantiation
        jsBuilder.append("if (typeof(");
        jsBuilder.append(JS_CONSTANT);
        jsBuilder.append(") === 'undefined') {\n");
        jsBuilder.append("\tvar ");
        jsBuilder.append(JS_CONSTANT);
        jsBuilder.append(" = {};\n");
        jsBuilder.append("}\n");

        for (String name : regions.keySet()) {
            final Region reg = regions.get(name);
            
            // Instruction that affect the new object to the WUIC_SPRITE constant
            jsBuilder.append(JS_CONSTANT);
            jsBuilder.append("['");
            jsBuilder.append(convertAllowedName(heapId, name));
            jsBuilder.append("'] = {\n\tx : \"");
            jsBuilder.append(reg.getxPosition());
            jsBuilder.append("\",\n\ty : \"");
            jsBuilder.append(reg.getyPosition());
            jsBuilder.append("\",\n\tw : \"");
            jsBuilder.append(reg.getWidth());
            jsBuilder.append("\",\n\th : \"");
            jsBuilder.append(reg.getHeight());
            jsBuilder.append("\",\n\turl : \"");
            jsBuilder.append(IOUtils.mergePath("/", url, retval.getVersionNumber().toString(), image));
            jsBuilder.append("\"\n};\n");
        }

        // Make a byte array and return nut wrapper
        retval.setBytes(jsBuilder.toString().getBytes());

        return retval;
    }
}