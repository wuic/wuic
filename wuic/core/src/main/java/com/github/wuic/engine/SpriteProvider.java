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

package com.github.wuic.engine;

import com.github.wuic.NutTypeFactory;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Source;
import com.github.wuic.util.UrlProvider;

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
 * {@link com.github.wuic.NutType}.
 * </p>
 * 
 * @author Guillaume DROUET
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
     * a {@link com.github.wuic.NutType} as key a {@link com.github.wuic.nut.Nut} as value.
     * </p>
     * 
     * <p>
     * Each sprite is in a path associated to a supported
     * {@link com.github.wuic.NutType} which helps
     * to determinate the language used to represent them. 
     * </p>
     * 
     * @param workflowId the workflow ID
     * @param urlProvider the {@link UrlProvider}
     * @param nutNamePrefix the prefix to append to the sprite nut name
     * @param source the original nuts
     * @param nutTypeFactory the nut type factory
     * @return a nut representing the sprite path
     * @throws java.io.IOException if an I/O error occurs while aggregating images
     */
    ConvertibleNut getSprite(String workflowId, UrlProvider urlProvider, String nutNamePrefix, Source source, NutTypeFactory nutTypeFactory)
            throws IOException;

    /**
     * <p>
     * Initializes this provider with a new image. Previous regions will be cleared.
     * </p>
     * 
     * @param nut the new image
     */
    void init(ConvertibleNut nut);
}
