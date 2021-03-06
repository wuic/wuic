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

import com.github.wuic.engine.Region;
import com.github.wuic.engine.SpriteProvider;
import com.github.wuic.nut.ConvertibleNut;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * Abstraction of the implementation of what is a {@link SpriteProvider}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.3.1
 */
public abstract class AbstractSpriteProvider implements SpriteProvider {

    /**
     * Region in the image.
     */
    private Map<String, Region> regions;

    /**
     * The image.
     */
    private ConvertibleNut image;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    public AbstractSpriteProvider() {
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
    public void init(final ConvertibleNut nut) {
        regions.clear();
        image = nut;
    }

    /**
     * <p>
     * Unified method to use to compute allowed name in sprite declaration.
     * </p>
     *
     * <p>
     * First of all, the heap ID is concatenated with the name separated by an underscore. The extension and parent path
     * of the name will be removed. Finally, any character which is not a letter (in upper or lower case) will be replaced
     * by an "underscore".
     * </p>
     *
     * @param name the name to use in sprite
     * @return the name usable in sprites
     */
    public String convertAllowedName(final String heapId, final String name) {
        // Class name is based on the name without the directory section and the path extension
        final int start = name.lastIndexOf('/') + 1;
        final int last = name.lastIndexOf('.');

        // Build the string
        final StringBuilder retval = new StringBuilder();
        retval.append(heapId);
        retval.append("_");
        retval.append(name.substring(start, last > start ? last : name.length()));

        // Replace non letter characters
        return retval.toString().replaceAll("[^a-zA-Z]", "_");
    }

    /**
     * <p>
     * Gets the regions.
     * </p>
     *
     * @return teh regions
     */
    protected Map<String, Region> getRegions() {
        return regions;
    }

    /**
     * <p>
     * Gets the image.
     * </p>
     *
     * @return the image
     */
    protected ConvertibleNut getImage() {
        return image;
    }
}
