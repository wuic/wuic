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

import com.github.wuic.EnumNutType;
import com.github.wuic.NutType;
import com.github.wuic.config.Alias;
import com.github.wuic.config.Config;
import com.github.wuic.config.ObjectConfigParam;
import com.github.wuic.engine.DimensionPacker;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.Region;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.ImageNut;
import com.github.wuic.nut.InMemoryNut;
import com.github.wuic.nut.Source;
import com.github.wuic.nut.SourceImpl;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Input;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import static com.github.wuic.ApplicationConfig.PACKER_CLASS_NAME;

/**
 * <p>
 * This engine is in charge to merge images into one final image.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.2.0
 */
@EngineService(injectDefaultToWorkflow = true, isCoreEngine = true)
@Alias("imageAggregator")
public class ImageAggregatorEngine extends AbstractAggregatorEngine {

    /**
     * The path name when images are aggregated.
     */
    public static final String AGGREGATION_NAME = AbstractAggregatorEngine.aggregationName(EnumNutType.PNG.getExtensions());

    /**
     * The default packer.
     */
    private static final String DEFAULT_PACKER = "com.github.wuic.engine.core.BinPacker";

    /**
     * Dimension packer.
     */
    private DimensionPacker<ConvertibleNut> dimensionPacker;

    /**
     * <p>
     * Initializes a new aggregator engine.
     * </p>
     *
     * @param packer the packer
     */
    @Config
    public void init(@ObjectConfigParam(defaultValue = DEFAULT_PACKER, propertyKey = PACKER_CLASS_NAME) final DimensionPacker<ConvertibleNut> packer) {
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
            final Source source = new SourceImpl();

            // Initializing the final image
            final Dimension finalDim = getDimensionPack();
            final BufferedImage transparentImage = makeTransparentImage((int) finalDim.getWidth(), (int) finalDim.getHeight());

            // Merge each image into the final image
            for (final Entry<Region, ConvertibleNut> entry : packed.entrySet()) {
                Input is = null;

                try {
                    is = entry.getValue().openStream();
                    final BufferedImage buff = ImageIO.read(is.inputStream());
                    final Region r = entry.getKey();
                    transparentImage.createGraphics().drawImage(buff, r.getxPosition(), r.getyPosition(), null);

                    source.addOriginalNut(new ImageNut(entry.getValue(), r));
                } catch (IOException ioe) {
                    WuicException.throwWuicException(ioe);
                } finally {
                    IOUtils.close(is);
                }
            }

            // Write the generated image as a WUIC nut to return it
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                ImageIO.write(transparentImage, "png", bos);
            } catch (IOException ioe) {
                WuicException.throwWuicException(ioe);
            }

            final ConvertibleNut res = new InMemoryNut(bos.toByteArray(),
                    AGGREGATION_NAME, getNutTypeFactory().getNutType(EnumNutType.PNG),
                    source,
                    NutUtils.getVersionNumber(source.getOriginalNuts()));

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
        return Arrays.asList(getNutTypeFactory().getNutType(EnumNutType.PNG));
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
            Input is = null;

            try {
                is = nut.openStream();
                final BufferedImage buff = ImageIO.read(is.inputStream());

                dimensionPacker.addElement(new Dimension(buff.getWidth(), buff.getHeight()), nut);
            } catch (IOException ioe) {
                WuicException.throwWuicException(ioe);
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
