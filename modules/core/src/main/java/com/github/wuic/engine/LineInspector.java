/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Represents an object providing replacement functionality inside a line for a group of character matching a particular
 * pattern.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.3.3
 */
public abstract class LineInspector {

    /**
     * The pattern.
     */
    private Pattern pattern;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param p the pattern.
     */
    public LineInspector(final Pattern p) {
        pattern = p;
    }

    /**
     * <p>
     * Manages the given nut that corresponds to the specified referenced path and append the transformation with proper
     * path.
     * </p>
     *
     * @param nut the nut retrieved with path
     * @param replacement the string builder to append
     * @param request the engine request
     * @param heap the heap that contains the original nut
     * @param skippedEngine the engine to skip when processing resulting nuts
     * @return the processed nuts specified in parameter
     * @throws WuicException if processing fails
     */
    public static List<Nut> manageAppend(final Nut nut,
                                         final StringBuilder replacement,
                                         final EngineRequest request,
                                         final NutsHeap heap,
                                         final EngineType ... skippedEngine) throws WuicException {
        List<Nut> res;

        // If nut name is null, it means that nothing has been changed by the inspector
        res = Arrays.asList(nut);

        // Process nut
        final NodeEngine engine = request.getChainFor(nut.getNutType());
        if (engine != null) {
            res = engine.parse(new EngineRequest(res, heap, request, skippedEngine));
        }

        // Use proxy URI if DAO provide it
        final String proxy = nut.getProxyUri();

        final Nut resNut = res.isEmpty() ? null: res.get(0);
        final Nut renamed;

        if (resNut != null) {
            renamed = new CompositeNut(resNut.getName().replace("../", "a/../"), null, resNut);
            res.set(0, renamed);
        } else {
            renamed = nut;
        }

        if (proxy == null) {
            replacement.append(IOUtils.mergePath(
                    "/",
                    request.getContextPath(),
                    request.getWorkflowId(),
                    String.valueOf(NutUtils.getVersionNumber(nut)),
                    renamed.getName()));
        } else {
            replacement.append(proxy);
        }

        return res;
    }

    /**
     * <p>
     * Gets the pattern to find text to be replaced inside the lines.
     * </p>
     *
     * @return the pattern to use
     */
    public final Pattern getPattern() {
        return pattern;
    }

    /**
     * <p>
     * Computes the replacement to be made inside the text for the given {@code Matcher} which its {@code find()}
     * method as just been called.
     * </p>
     *
     * @param matcher the matcher which provides found text thanks to its {@code group()} method.
     * @param replacement the text which will replace the matching text
     * @param request the request that orders this transformation
     * @param heap use when we need to create nut
     * @param originalNut the original nut
     * @return the nut that was referenced in the matching text, {@code null} if the inspector did not perform any change
     * @throws WuicException if an exception occurs
     */
    public abstract List<Nut> appendTransformation(Matcher matcher,
                                                   StringBuilder replacement,
                                                   EngineRequest request,
                                                   NutsHeap heap,
                                                   Nut originalNut) throws WuicException;
}
