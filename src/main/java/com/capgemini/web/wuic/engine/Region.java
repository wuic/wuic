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

import java.awt.Dimension;

/**
 * <p>
 * Represents a region to use when need to specify a sprite.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.2.0
 */
public class Region extends Dimension implements Comparable<Dimension> {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -4451577876338494519L;

    /**
     * The x position.
     */
    private int xPosition;
    
    /**
     * The y position.
     */
    private int yPosition;
    
    /**
     * <p>
     * Builds a new region.
     * </p>
     * 
     * @param x the x position
     * @param y the y position
     * @param w the width
     * @param h the height
     */
    public Region(final int x, final int y, final int w, final int h) {
        super(w, h);
        xPosition = x;
        yPosition = y;
    }

    /**
     * <p>
     * Builds a new region.
     * </p>
     * 
     * @param x the x position
     * @param y the y position
     * @param d the region's dimensions
     */
    public Region(final int x, final int y, final Dimension d) {
        super(d);
        xPosition = x;
        yPosition = y;
    }

    /**
     * <p>
     * Builds a region with the area represented by the given region.
     * </p>
     * 
     * @param other the given region
     */
    public Region(final Region other) {
        this(other.xPosition, other.yPosition, other);
    }
    
    /**
     * <p>
     * Gets the x position.
     * </p>
     *
     * @return the x position
     */
    public int getxPosition() {
        return xPosition;
    }

    /**
     * <p>
     * Gets the y position.
     * </p>
     * 
     * @return the y position
     */
    public int getyPosition() {
        return yPosition;
    }

    /**
     * <p>
     * Compares this region to another dimension.
     * </p>
     * 
     * <p>
     * This region is bigger than the compared dimension if and only if its
     * width AND its height are bigger than the other one.
     * </p>
     * 
     * @param other the other dimension
     * @return a positive, zero or negative value indicating the result of comparison
     */
    @Override
    public int compareTo(final Dimension other) {
        return (int) ((this.width * this.height) - (other.getWidth() * other.getHeight()));
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(final Object other) {
        if (other instanceof Region) {
            return super.equals(other) && ((Region) other).xPosition == xPosition && ((Region) other).yPosition == yPosition;
        } else {
            return Boolean.FALSE;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return super.hashCode();
    }
}
