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

import java.awt.Dimension;
import java.util.Map;

/**
 * <p>
 * A packer packs a set of dimensions (2d) in the smalled area.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.2.3
 * @param <T> a type of data associated to the dimensions
 */
public interface DimensionPacker<T> {

    /**
     * <p>
     * Adds a new dimension with its associated data to the elements to be packed.
     * </p>
     * 
     * @param dimension the dimension
     * @param data the data.
     */
    void addElement(Dimension dimension, T data);
    
    /**
     * <p>
     * Clears all the elements previously added.
     * </p>
     */
    void clearElements();
    
    /**
     * <p>
     * Packs and returns the regions which result of the computation of the dimension
     * indicated thanks to the {@link DimensionPacker#addElement(Dimension, Object)}
     * method.
     * </p>
     * 
     * @return the packed regions
     */
    Map<Region, T> getRegions();

    /**
     * <p>
     * Returns the area which contains all the filled zones.
     * </p>
     * 
     * @return the filled area
     */
    Dimension getFilledArea();
}
