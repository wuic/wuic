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
import com.capgemini.web.wuic.configuration.BadConfigurationException;
import com.capgemini.web.wuic.configuration.Configuration;
import com.capgemini.web.wuic.configuration.ImageConfiguration;
import com.capgemini.web.wuic.engine.PackerEngine;
import com.capgemini.web.wuic.engine.Region;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

/**
 * <p>
 * This engine is in charge to merge images into one final image.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.2.0
 */
public class CGImageAggregatorEngine extends PackerEngine {

    /**
     * The file name when images are aggregated.
     */
    public static final String AGGREGATION_NAME = "aggregation.png";
    
    /**
     * The configuration.
     */
    private ImageConfiguration configuration;
    
    /**
     * <p>
     * Builds a new aggregator engine.
     * </p>
     * 
     * @param config the configuration
     * @throws BadConfigurationException if a bad configuration is detected
     */
    public CGImageAggregatorEngine(final Configuration config)
            throws BadConfigurationException {
        if (config instanceof ImageConfiguration) {
            configuration = (ImageConfiguration) config;
            setDimensionPacker(configuration.createDimensionPacker());
        } else {
            final String message = config + " must be an instance of " + ImageConfiguration.class.getName();
            throw new IllegalArgumentException(message);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> parse(final List<WuicResource> files)
            throws IOException {
        /*
         * Do nothing if the configuration says that no aggregation should be done
         */
        if (!works()) {
            return files;
        } else {
            final Map<Region, WuicResource> packed = pack(files);
    
            // Initializing the final image  
            final Dimension finalDim = getDimensionPack();
            final BufferedImage transparentImage = makeTransparentImage((int) finalDim.getWidth(), (int) finalDim.getHeight());        
            
            // Merge each image into the final image
            for (Entry<Region, WuicResource> entry : packed.entrySet()) {
                InputStream is = null;
              
                try {
                    is = entry.getValue().openStream();
                    final BufferedImage buff = ImageIO.read(is);
                    final Region r = entry.getKey();
                    transparentImage.createGraphics().drawImage(buff, r.getxPosition(), r.getyPosition(), null);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
            
            // Write the generated image as a WUIC resource to return it
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(transparentImage, "png", bos);
            final WuicResource res = new ByteArrayWuicResource(bos.toByteArray(), AGGREGATION_NAME, FileType.PNG);
            
            return Arrays.asList(res);
        }
    }
    
    /**
     * <p>
     * Makes a transparent image of the given dimensions.
     * </p>
     *
     * @param width the image width
     * @param height the image height
     * @return transparent image
     */
    public static BufferedImage makeTransparentImage(final int width, final int height) {
        // Create an image with the given dimension
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Now filter the image to make all pixels transparent
        final ImageFilter filter = new TransparentImageFilter();
        final ImageProducer ip = new FilteredImageSource(img.getSource(), filter);
        final Image image = Toolkit.getDefaultToolkit().createImage(ip);
        
        // Write the resulting image in the buffered image to return
        final BufferedImage bufferedImage = new BufferedImage(width, height, img.getType());
        final Graphics graphics = bufferedImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        
        return bufferedImage;
    }
    
    /**
     * <p>
     * This filter helps make an image transparent.
     * </p>
     * 
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.2.0
     */
    private static class TransparentImageFilter extends RGBImageFilter {
        
        /**
         * {@inheritDoc}
         */
        @Override
        public final int filterRGB(final int x, final int y, final int rgb) {
            return 0x00FFFFFF & rgb;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return configuration.aggregate();
    }
}
