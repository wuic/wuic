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

package com.capgemini.web.wuic.engine;

import com.capgemini.web.wuic.WuicResource;

import java.io.IOException;

/**
 * <p>
 * This interface represents a type of object which provides sprite generation
 * capabilities.
 * </p>
 * 
 * <p>
 * When a set of images is aggregated, then the region of each of them in the
 * generated image is located through a sprite. A sprite could be generated
 * according to specific convention in a chosen language. The implementation
 * of this interface must be able to specify the language through a known
 * {@link FileType}. 
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.2.0
 */
public interface SpriteProvider {

    /**
     * <p>
     * Registers a new region in the image.
     * </p>
     * 
     * @param region the region
     * @param name the region name
     */
    void addRegion(Region region, String name);
    
    /**
     * <p>
     * Get the sprite of all the added images in a map associating
     * a {@link FileType} as key a {@link WuicResource} as value.
     * </p>
     * 
     * <p>
     * Each sprite is in a file associated to a supported
     * {@link com.capgemini.web.wuic.FileType} which helps
     * to determinate the language used to represent them. 
     * </p>
     * 
     * @param url of the final image
     * @return a resource representing the sprite file
     * @throws IOException if an I/O error occurs while aggregating images 
     */
    WuicResource getSprite(String url) throws IOException;

    /**
     * <p>
     * Initializes this provider with a new image. Previous regions will be cleared.
     * </p>
     * 
     * @param imageName the new image name
     */
    void init(String imageName);
}
