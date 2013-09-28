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


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.NutType;
import com.github.wuic.engine.*;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.core.ByteArrayNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;

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
 * @version 1.4
 * @since 0.2.0
 */
public class CGImageAggregatorEngine extends Engine {

    /**
     * The path name when images are aggregated.
     */
    public static final String AGGREGATION_NAME = "aggregate.png";

    /**
     * Activate aggregation or not.
     */
    private Boolean doAggregation;

    /**
     * The sprite provider.
     */
    private SpriteProvider[] spriteProviders;

    /**
     * Dimension packer.
     */
    private DimensionPacker<Nut> dimensionPacker;

    /**
     * <p>
     * Builds a new aggregator engine.
     * </p>
     *
     * @param aggregate if aggregation should be activated or not
     * @param packer the packer which packs the images
     * @param sp the provider which generates sprites
     */
    public CGImageAggregatorEngine(final Boolean aggregate, final DimensionPacker<Nut> packer, final SpriteProvider[] sp) {
        doAggregation = aggregate;
        spriteProviders = Arrays.copyOf(sp, sp.length);
        dimensionPacker = packer;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> internalParse(final EngineRequest request) throws WuicException {
        // Only used if sprite provider is not null
        int spriteCpt = 0;

        // If the configuration says that no aggregation should be done, keep all images separated
        if (!works()) {
            // If a sprite provider exists, compute one nut for each image and link them
            if (spriteProviders != null) {
                final List<Nut> retval = new ArrayList<Nut>();
                final String url = IOUtils.mergePath(request.getContextPath(), request.getWorkflowId());

                // Calculate type and dimensions of the final image
                for (final Nut n : request.getNuts()) {
                    // Clear previous work
                    initSpriteProviders(n.getName());
                    InputStream is = null;

                    try {
                        is = n.openStream();

                        final BufferedImage buff = ImageIO.read(is);
                        addRegionToSpriteProviders(new Region(0, 0, buff.getWidth() - 1, buff.getHeight() - 1), n.getName());

                        // Process referenced nut
                        applySpriteProviders(url, request.getHeap().getId(), String.valueOf(spriteCpt++), n, request);

                        retval.add(n);
                    } catch (IOException ioe) {
                        throw new StreamException(ioe);
                    } finally {
                        IOUtils.close(is);
                    }
                }

                return retval;
            } else {
                return request.getNuts();
            }
        } else {
            // Clear previous work
            if (spriteProviders != null) {
                initSpriteProviders(CGImageAggregatorEngine.AGGREGATION_NAME);
            }

            final Map<Region, Nut> packed = pack(request.getNuts());
    
            // Initializing the final image  
            final Dimension finalDim = getDimensionPack();
            final BufferedImage transparentImage = makeTransparentImage((int) finalDim.getWidth(), (int) finalDim.getHeight());        

            // Merge each image into the final image
            for (final Entry<Region, Nut> entry : packed.entrySet()) {
                // Register the region to the sprite provider
                if (spriteProviders != null) {
                    addRegionToSpriteProviders(entry.getKey(), entry.getValue().getName());
                }

                InputStream is = null;
              
                try {
                    is = entry.getValue().openStream();
                    final BufferedImage buff = ImageIO.read(is);
                    final Region r = entry.getKey();
                    transparentImage.createGraphics().drawImage(buff, r.getxPosition(), r.getyPosition(), null);
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

            final Nut res = new ByteArrayNut(bos.toByteArray(), AGGREGATION_NAME, NutType.PNG);

            if (spriteProviders != null) {
                final String url = IOUtils.mergePath(request.getContextPath(), request.getWorkflowId());

                // Process referenced nut
                applySpriteProviders(url, request.getHeap().getId(), String.valueOf(spriteCpt), res, request);
            }

            return Arrays.asList(res);
        }
    }

    /**
     * <p>
     * Initializes all sprite providers.
     * </p>
     *
     * @param name the name
     */
    private void initSpriteProviders(final String name) {
        for (final SpriteProvider sp : spriteProviders) {
            sp.init(name);
        }
    }

    /**
     * <p>
     * Adds the region to all sprite providers.
     * </p>
     *
     * @param region the region
     * @param name the region name
     */
    private void addRegionToSpriteProviders(final Region region, final String name) {
        for (final SpriteProvider sp : spriteProviders) {
            sp.addRegion(region, name);
        }
    }

    /**
     * <p>
     * Generates sprites from all sprite providers and add it to the given nut.
     * </p>
     *
     * @param url the base URL
     * @param heapId the HEAP id
     * @param suffix the name suffix
     * @param n the nut
     * @param request the initial engine request
     * @throws WuicException if generation fails
     */
    private void applySpriteProviders(final String url, final String heapId, final String suffix, final Nut n, final EngineRequest request)
        throws WuicException {
        for (final SpriteProvider sp : spriteProviders) {
            final Nut nut = sp.getSprite(url, heapId, suffix);
            final Engine chain = request.getChainFor(nut.getNutType());

            if (chain != null) {
                /*
                 * We perform request by skipping cache to not override cache entry with the given heap ID as key.
                 * We also skip inspection because this is not necessary to detect references to this image
                 */
                final List<Nut> parsed = chain.parse(new EngineRequest(heapId, Arrays.asList(nut), request, EngineType.CACHE, EngineType.INSPECTOR));
                n.addReferencedNut(parsed.get(0));
            } else {
                n.addReferencedNut(nut);
            }
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
    public Boolean works() {
        return doAggregation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.PNG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.AGGREGATOR;
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
    public Map<Region, Nut> pack(final List<Nut> nuts) throws WuicException {

        // Clear previous work
        dimensionPacker.clearElements();

        // Load each image, read its dimension and add it to the packer with the ile as data
        for (final Nut nut : nuts) {
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
