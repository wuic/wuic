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

package com.github.wuic.engine;

import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.util.IOUtils;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * <p>
 * Abstract engine which could be extended by engines which uses a packing algorithm
 * with an implementation provided by a {@link com.github.wuic.engine.DimensionPacker}.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.2.0
 */
public abstract class PackerEngine extends Engine {

    /**
     * Dimension packer.
     */
    private DimensionPacker<WuicResource> dimensionPacker;

    /**
     * <p>
     * Sets the {@link DimensionPacker} to use.
     * </p>
     * 
     * @param packer the packer
     */
    public void setDimensionPacker(final DimensionPacker<WuicResource> packer) {
        dimensionPacker = packer;
    }
    
    /**
     * <p>
     * Packs the given resources (which embed images) in the smallest area.
     * </p>
     * 
     * @param files the images to pack
     * @return a map which associates each packed image to its allocated region
     * @throws WuicException if one image could not be read
     */
    public Map<Region, WuicResource> pack(final List<WuicResource> files) throws WuicException {

        // Clear previous work
        dimensionPacker.clearElements();
        
        // Load each image, read its dimension and add it to the packer with the ile as data
        for (WuicResource file : files) {
            InputStream is = null;
            
            try {
                is = file.openStream();
                final BufferedImage buff = ImageIO.read(is);
                
                dimensionPacker.addElement(new Dimension(buff.getWidth(), buff.getHeight()), file);
            } catch (IOException ioe) {
                throw new StreamException(ioe);
            } finally {
                IOUtils.close(is);
            }
        }
        
        // Get the regions calculated by the packer !
        return dimensionPacker.getRegions();
    }
    
    /**
     * <p>
     * Gets the dimension computed by the packer when defining a position for
     * elements. 
     * </p>
     * 
     * @return the packed dimension
     */
    public Dimension getDimensionPack() {
        return dimensionPacker.getFilledArea();
    }
}
