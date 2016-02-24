/*
 * "Copyright (c) 2016   Capgemini Technology Services (hereinafter "Capgemini")
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
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.engine.Region;
import com.github.wuic.engine.SpriteProvider;
import com.github.wuic.engine.setter.SpriteProviderPropertySetter;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.ImageNut;
import com.github.wuic.nut.Source;
import com.github.wuic.nut.SourceImpl;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.UrlProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * <p>
 * This engine computes sprites for a set of images. It let the next engine do their job and get the result to analyze
 * a potential aggregated image.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
@EngineService(injectDefaultToWorkflow = true, isCoreEngine = true)
public class SpriteInspectorEngine extends NodeEngine {

    /**
     * The sprite provider.
     */
    private SpriteProvider[] spriteProviders;

    /**
     * Perform inspection or not.
     */
    private Boolean doInspection;

    /**
     * <p>
     * Builds a new aggregator engine.
     * </p>
     *
     * @param inspect if inspect or not
     * @param sp the provider which generates sprites
     */
    @ConfigConstructor
    public SpriteInspectorEngine(
            @BooleanConfigParam(
                    defaultValue = true,
                    propertyKey = ApplicationConfig.INSPECT)
            final Boolean inspect,
            @ObjectConfigParam(
                    defaultValue = "css",
                    propertyKey = ApplicationConfig.SPRITE_PROVIDER_CLASS_NAME,
                    setter = SpriteProviderPropertySetter.class)
            final SpriteProvider[] sp) {
        spriteProviders = Arrays.copyOf(sp, sp.length);
        doInspection = inspect;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ConvertibleNut> internalParse(final EngineRequest request) throws WuicException {
        /*
         * If the configuration says that no inspection should be done or if no sprite provider is defined,
         * then we return the given request nuts
         */
        return !works() || spriteProviders.length == 0 ? request.getNuts() : process(request);
    }

    /**
     * <p>
     * Process the given request.
     * </p>
     *
     * @param request the request
     * @return the result
     * @throws WuicException if process fails
     */
    private List<ConvertibleNut> process(final EngineRequest request) throws WuicException {
        int spriteCpt = 0;

        // Isolate nuts excluded from sprite computation
        final List<ConvertibleNut> retval = new ArrayList<ConvertibleNut>();
        final List<ConvertibleNut> nuts = new ArrayList<ConvertibleNut>();
        split(request, nuts, retval);

        if (nuts.isEmpty()) {
            return retval;
        }

        final List<ConvertibleNut> res = getNext() == null ?
                nuts : getNext().parse(new EngineRequestBuilder(request).nuts(nuts).build());

        // Calculate type and dimensions of the final image
        for (final ConvertibleNut n : res) {
            if (request.isBestEffort()) {
                n.setNutName(IOUtils.mergePath("best-effort", n.getName()));
            }

            // Clear previous work
            initSpriteProviders(n);
            processNut(n);

            // Process referenced nut
            final String suffix;

            if (request.getPrefixCreatedNut().isEmpty()) {
                suffix  = String.valueOf(spriteCpt++);
            } else {
                suffix  = IOUtils.mergePath(request.getPrefixCreatedNut(), String.valueOf(spriteCpt++));
            }

            retval.add(applySpriteProviders(suffix, n, request));
        }

        return retval;
    }

    /**
     * <p>
     * Processes the given nut.
     * </p>
     *
     * @param nut the nut
     * @throws WuicException if processing fails
     */
    private void processNut(final ConvertibleNut nut) throws WuicException {

        // Aggregated nut references all single images
        if (!nut.getSource().getOriginalNuts().isEmpty()) {
            final List<ConvertibleNut> originalNuts = nut.getSource().getOriginalNuts();

            for (final ConvertibleNut origin : originalNuts) {
                if (origin instanceof ImageNut) {
                    addRegionToSpriteProviders(ImageNut.class.cast(origin).getRegion(), origin.getName());
                } else {
                    WuicException.throwBadArgumentException(
                            new IllegalArgumentException("Processed nuts must refer ImageNut instances as original nuts"));
                }
            }
        } else {
            // Aggregation is not activated
            InputStream is = null;

            try {
                is = nut.openStream();
                final ImageInputStream iis = ImageIO.createImageInputStream(is);

                ImageReader reader = ImageIO.getImageReaders(iis).next();
                reader.setInput(iis);
                addRegionToSpriteProviders(new Region(0, 0, reader.getWidth(0) - 1, reader.getHeight(0) - 1), nut.getName());
            } catch (IOException ioe) {
                WuicException.throwWuicException(ioe);
            } finally {
                IOUtils.close(is);
            }
        }
    }

    /**
     * <p>
     * Splits the request nuts in two list to separate nuts excluded from sprite computation from others.
     * </p>
     *
     * @param request the request
     * @param computeSprite nuts to include in sprite computation
     * @param excludeSprite nuts to exclude from sprite computation
     * @throws WuicException if parsing fails
     */
    private void split(final EngineRequest request,
                       final List<ConvertibleNut> computeSprite,
                       final List<ConvertibleNut> excludeSprite) throws WuicException {
        for (final ConvertibleNut nut : request.getNuts()) {
            if (request.isExcludedFromSpriteComputation(nut)) {
                if (getNext() == null) {
                    excludeSprite.add(nut);
                } else {
                    final EngineRequest r = new EngineRequestBuilder(request).nuts(Arrays.asList(nut)).skip(EngineType.AGGREGATOR).build();
                    excludeSprite.addAll(getNext().parse(r));
                }
            } else {
                computeSprite.add(nut);
            }
        }
    }

    /**
     * <p>
     * Initializes all sprite providers.
     * </p>
     *
     * @param nut the nut
     */
    private void initSpriteProviders(final ConvertibleNut nut) {
        for (final SpriteProvider sp : spriteProviders) {
            sp.init(nut);
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
     * @param suffix the name suffix
     * @param n the nut
     * @param request the initial engine request
     * @throws WuicException if generation fails
     */
    private ConvertibleNut applySpriteProviders(final String suffix,
                                                final ConvertibleNut n,
                                                final EngineRequest request)
            throws WuicException {
        if (spriteProviders.length == 0) {
            return n;
        }

        ConvertibleNut retval = null;

        for (final SpriteProvider sp : spriteProviders) {
            final ConvertibleNut nut;

            try {
                final String group = request.getHeap().findHeapFor(n).getId();
                final String basePath = IOUtils.mergePath("/", request.getContextPath(), request.getWorkflowId());
                final UrlProvider urlProvider = request.getUrlProviderFactory().create(basePath);
                final Source source = new SourceImpl();

                nut = sp.getSprite(group, urlProvider, suffix, source);
                nut.addReferencedNut(n);
            } catch (IOException ioe) {
                WuicException.throwWuicException(ioe);
                return null;
            }

            final NodeEngine chain = request.getChainFor(nut.getInitialNutType());

            if (chain != null) {
                /*
                 * We perform request by skipping cache to not override cache entry with the given heap ID as key.
                 * We also skip inspection because this is not necessary to detect references to this image
                 */
                final EngineType[] skip = request.alsoSkip(EngineType.CACHE, EngineType.INSPECTOR);
                final List<ConvertibleNut> parsed = chain.parse(new EngineRequestBuilder(request)
                        .nuts(Arrays.asList(nut))
                        .skip(skip)
                        .build());

                if (retval != null) {
                    n.addReferencedNut(parsed.get(0));
                } else {
                    retval = parsed.get(0);
                    retval.addTransformer(new AddReferencedNutOnTransform(n));
                }
            } else if (retval != null) {
                n.addTransformer(new AddReferencedNutOnTransform(nut));
            }  else {
                retval = nut;
                retval.addTransformer(new AddReferencedNutOnTransform(n));
            }
        }

        return retval == null ? n : retval;
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
    public Boolean works() {
        return doInspection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.INSPECTOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean callNextEngine() {
        return false;
    }

    /**
     * <p>
     * This class adds a specified nut as a referenced nut when the
     * {@link com.github.wuic.util.Pipe.Transformer#transform(java.io.InputStream, java.io.OutputStream, Object)}
     * method is invoked.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.0
     */
    private static final class AddReferencedNutOnTransform extends Pipe.DefaultTransformer<ConvertibleNut> {

        /**
         * The referenced nut.
         */
        private ConvertibleNut ref;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param cn the nut to add as a referenced nut
         */
        private AddReferencedNutOnTransform(final ConvertibleNut cn) {
            this.ref = cn;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void transform(final InputStream is, final OutputStream os, final ConvertibleNut convertible) throws IOException {
            convertible.addReferencedNut(ref);
            super.transform(is, os, convertible);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean canAggregateTransformedStream() {
            return false;
        }
    }
}
