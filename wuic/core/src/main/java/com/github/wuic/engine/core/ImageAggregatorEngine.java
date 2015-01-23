/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.engine.core;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.engine.DimensionPacker;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.Region;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.ImageNut;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;

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
import java.util.ArrayList;
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
 * @version 1.8
 * @since 0.2.0
 */
@EngineService(injectDefaultToWorkflow = true, isCoreEngine = true)
public class ImageAggregatorEngine extends AbstractAggregatorEngine {

    /**
     * The path name when images are aggregated.
     */
    public static final String AGGREGATION_NAME = AbstractAggregatorEngine.aggregationName(NutType.PNG);

    /**
     * Dimension packer.
     */
    private DimensionPacker<ConvertibleNut> dimensionPacker;

    /**
     * <p>
     * Builds a new aggregator engine.
     * </p>
     *
     * @param aggregate if aggregation should be activated or not
     */
    @ConfigConstructor
    public ImageAggregatorEngine(
            @BooleanConfigParam(
                    defaultValue = true,
                    propertyKey = ApplicationConfig.AGGREGATE)
            final Boolean aggregate,
            @ObjectConfigParam(
                    defaultValue = "com.github.wuic.engine.core.BinPacker",
                    propertyKey = ApplicationConfig.PACKER_CLASS_NAME)
            final DimensionPacker<ConvertibleNut> packer) {
        super(aggregate);
        dimensionPacker = packer;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> aggregationParse(final EngineRequest request) throws WuicException {
        // If the configuration says that no aggregation should be done, keep all images separated
        if (!works()) {
            return request.getNuts();
        } else {
            final Map<Region, ConvertibleNut> packed = pack(request.getNuts());
            final List<ConvertibleNut> originals = new ArrayList<ConvertibleNut>(packed.size());

            // Initializing the final image
            final Dimension finalDim = getDimensionPack();
            final BufferedImage transparentImage = makeTransparentImage((int) finalDim.getWidth(), (int) finalDim.getHeight());

            // Merge each image into the final image
            for (final Entry<Region, ConvertibleNut> entry : packed.entrySet()) {
                InputStream is = null;

                try {
                    is = entry.getValue().openStream();
                    final BufferedImage buff = ImageIO.read(is);
                    final Region r = entry.getKey();
                    transparentImage.createGraphics().drawImage(buff, r.getxPosition(), r.getyPosition(), null);

                    originals.add(new ImageNut(entry.getValue(), r));
                } catch (IOException ioe) {
                    throw new StreamException(ioe);
                } finally {
                    IOUtils.close(is);
                }
            }

            // Write the generated image as a WUIC nut to return it
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                ImageIO.write(transparentImage, "png", bos);
            } catch (IOException ioe) {
                throw new StreamException(ioe);
            }

            final ConvertibleNut res = new ByteArrayNut(bos.toByteArray(), AGGREGATION_NAME, NutType.PNG, originals, NutUtils.getVersionNumber(originals));

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
     * @version 1.1
     * @since 0.2.0
     */
    private static class TransparentImageFilter extends RGBImageFilter {

        /**
         * Filter value.
         */
        private static final int FILTER_OFFSET = 0x00FFFFFF;

        /**
         * {@inheritDoc}
         */
        @Override
        public final int filterRGB(final int x, final int y, final int rgb) {
            return FILTER_OFFSET & rgb;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.PNG);
    }

    /**
     * <p>
     * Packs the given nuts (which embed images) in the smallest area.
     * </p>
     *
     * @param nuts the images to pack
     * @return a map which associates each packed image to its allocated region
     * @throws WuicException if one image could not be read
     */
    public Map<Region, ConvertibleNut> pack(final List<ConvertibleNut> nuts) throws WuicException {

        // Clear previous work
        dimensionPacker.clearElements();

        // Load each image, read its dimension and add it to the packer with the ile as data
        for (final ConvertibleNut nut : nuts) {
            InputStream is = null;

            try {
                is = nut.openStream();
                final BufferedImage buff = ImageIO.read(is);

                dimensionPacker.addElement(new Dimension(buff.getWidth(), buff.getHeight()), nut);
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
