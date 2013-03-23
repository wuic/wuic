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
 * •   The above copyright notice and this permission notice shall be included in
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


package com.capgemini.web.wuic.engine.impl.embedded;

import com.capgemini.web.wuic.ByteArrayWuicResource;
import com.capgemini.web.wuic.FileType;
import com.capgemini.web.wuic.WuicResource;
import com.capgemini.web.wuic.engine.Region;
import com.capgemini.web.wuic.engine.SpriteProvider;
import com.capgemini.web.wuic.xml.WuicXmlLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * <p>
 * This class provides sprite throughout javascript language. The javascript file
 * defines an array which associates each aggregated image name to its region in
 * the final image.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.2.0
 */
public class CGJavascriptSpriteProvider implements SpriteProvider {

    /**
     * The JACKSON MAPPER.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    /**
     * Region in the image.
     */
    private Map<String, Region> regions;
    
    /**
     * The image name.
     */
    private String image;
    
    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public CGJavascriptSpriteProvider() {
        regions = new LinkedHashMap<String, Region>();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addRegion(final Region region, final String name) {
        regions.put(name, region);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResource getSprite(final String url) throws IOException {
        final StringBuilder jsBuilder = new StringBuilder();
        
        for (String name : regions.keySet()) {
            final Region reg = regions.get(name);
            
            // Define region within the image
            final Map<String, String> region = new HashMap<String, String>(5);
            region.put("x", String.valueOf(reg.getxPosition()));
            region.put("y", String.valueOf(reg.getyPosition()));
            region.put("w", String.valueOf(reg.getWidth()));
            region.put("h", String.valueOf(reg.getHeight()));
            region.put("url", new StringBuilder(url).append("/?file=").append(image).toString());
        
            // Instruction that affect the new object to the WUIC_SPRITE constant
            jsBuilder.append("WUIC_SPRITE['");
            jsBuilder.append(name.replace("\'", "\\'"));
            jsBuilder.append("'] = ");
            jsBuilder.append(MAPPER.writeValueAsString(region));
            jsBuilder.append(";");
        }

        // Make a resource and return it
        final byte[] bytes = jsBuilder.toString().getBytes();
        final WuicResource spriteFile = new ByteArrayWuicResource(bytes, WuicXmlLoader.createGeneratedGroupId(regions.keySet()),
                FileType.JAVASCRIPT);
        
        return spriteFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final String imageName) {
        regions.clear();
        image = imageName;
    }
}