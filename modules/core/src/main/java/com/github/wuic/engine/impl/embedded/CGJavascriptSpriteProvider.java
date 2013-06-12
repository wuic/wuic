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

import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.resource.impl.ByteArrayWuicResource;
import com.github.wuic.FileType;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.engine.Region;
import com.github.wuic.util.StringUtils;

/**
 * <p>
 * This class provides sprite throughout javascript language. The javascript file
 * defines an array which associates each aggregated image name to its region in
 * the final image.
 * </p>
 *
 * <p>
 * In Javascript, an object is declared in variable starting with WUIC_SPRITE_
 * and ending with the group ID. The group ID is treated to be in upper case and
 * with its characters which are invalid in javascript variable names replaced
 * with an underscore.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.5
 * @since 0.2.0
 */
public class CGJavascriptSpriteProvider extends CGAbstractSpriteProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResource getSprite(final String url, final String groupId) throws StreamException {
        final StringBuilder jsBuilder = new StringBuilder();

        // Inject instantiation
        final String jsName = createJsName(groupId);
        jsBuilder.append("var ");
        jsBuilder.append(jsName);
        jsBuilder.append(" = {};");

        for (String name : regions.keySet()) {
            final Region reg = regions.get(name);
            
            // Instruction that affect the new object to the WUIC_SPRITE constant
            jsBuilder.append(jsName);
            jsBuilder.append("['");
            jsBuilder.append(name.replace("\'", "\\'"));
            jsBuilder.append("'] = {x : \"");
            jsBuilder.append(reg.getxPosition());
            jsBuilder.append("\", y : \"");
            jsBuilder.append(reg.getyPosition());
            jsBuilder.append("\", w : \"");
            jsBuilder.append(reg.getWidth());
            jsBuilder.append("\", h : \"");
            jsBuilder.append(reg.getHeight());
            jsBuilder.append("\", url : \"");
            jsBuilder.append(StringUtils.merge(new String[] { url, image, }, "/"));
            jsBuilder.append("\"};");
        }

        // Make a resource and return it
        final byte[] bytes = jsBuilder.toString().getBytes();
        return new ByteArrayWuicResource(bytes, "sprites.js", FileType.JAVASCRIPT);
    }

    /**
     * <p>
     * Computes the WUIC javascript constant name for the given ID.
     * </p>
     *
     * @param groupId the group ID
     * @return the JS name to use
     */
    private String createJsName(final String groupId) {
        return "WUIC_SPRITE_" + groupId.toUpperCase().replaceAll("\\W", "_");
    }
}