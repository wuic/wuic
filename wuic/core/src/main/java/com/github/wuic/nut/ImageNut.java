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


package com.github.wuic.nut;

import com.github.wuic.engine.Region;

/**
 * <p>
 * This class wraps a {@link Nut} that corresponds to an image and provide its position inside a combination.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
public class ImageNut extends NutWrapper {

    /**
     * The image region.
     */
    private Region region;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param image the nut to wrap
     * @param r the image's region
     */
    public ImageNut(final ConvertibleNut image, final Region r) {
        super(image);
        region = r;
    }

    /**
     * <p>
     * Gets the region.
     * </p>
     *
     * @return the region
     */
    public Region getRegion() {
        return region;
    }
}
