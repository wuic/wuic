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

import com.github.wuic.NutType;
import com.github.wuic.mbean.TransformationStat;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.Pipe;
import com.github.wuic.util.Pipe.Transformer;
import com.github.wuic.util.TimerTreeFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * <p>
 * A convertible nut has a content which can be transformed with a chain of {@link Transformer transformers}.
 * During transformation, the state of the nut can change and not only its content. For instance, the nut becomes
 * compressed or refers new nuts.
 * </p>
 *
 * <p>
 * Transformation process should change the state once. The implementation has to deal with this to not let the
 * transformers add duplicated data to the nut's state.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
public interface ConvertibleNut extends Nut {

    /**
     * <p>
     * When this nut is the result of the transformation of another one, then it is actually a processed nut.
     * This method returns a source object that provides the list of original nuts that have been transformed to obtain this nut.
     * When this nut is actually a non-transformed one, say an original nut, then the returned value won't provide any nut.
     * </p>
     *
     * <p>
     * The type of returned object can be extended to add more information about the sources (like source map).
     * </p>
     *
     * @return the source object of this nut, an object with an empty list if this nut is actually original
     */
    Source getSource();

    /**
     * <p>
     * Sets the source object for this nut.
     * </p>
     *
     * @param source the new source object
     */
    void setSource(Source source);

    /**
     * <p>
     * Returns the name potentially altered by other components like transformers.
     * </p>
     *
     * @return the nut name
     */
    String getName();

    /**
     * <p>
     * Returns the nut type potentially modified by other components like transformers.
     * </p>
     *
     * @return the path type
     */
    NutType getNutType();

    /**
     * <p>
     * Sets the nut name.
     * </p>
     *
     * @param nutName the name
     */
    void setNutName(String nutName);

    /**
     * <p>
     * Sets the nut type.
     * </p>
     *
     * @param nutType the new nut type
     */
    void setNutType(NutType nutType);

    /**
     * <p>
     * Transforms the source stream with all registered transformers and calls the given callback in addition
     * to callbacks previously registered. The method also writes the result to the given output stream.
     * </p>
     *
     * @param timerTreeFactory a timer tree factory to use when reporting elapsed time
     * @param onReady the callback
     * @return all the transformation statistics
     * @throws java.io.IOException if an I/O error occurs
     */
    Map<String, List<TransformationStat>> transform(TimerTreeFactory timerTreeFactory, Pipe.OnReady ... onReady) throws IOException;

    /**
     * <p>
     * Transforms the source stream as specified by {@link #transform(TimerTreeFactory, com.github.wuic.util.Pipe.OnReady...)}.
     * </p>
     *
     * @param onReady the callback
     * @return all the transformation statistics
     * @throws java.io.IOException if an I/O error occurs
     */
    Map<String, List<TransformationStat>> transform(Pipe.OnReady ... onReady) throws IOException;

    /**
     * <p>
     * When {@link #transform(com.github.wuic.util.Pipe.OnReady...)} is called execute the list of {@link Transformer transformers},
     * the {@code InputStream} can be a {@link com.github.wuic.nut.CompositeNut.CompositeInput} that should be treated
     * specifically by default. In fact, this type of stream is an aggregation of different streams that should be separately
     * transformed. This method tells the nut to not do this if it returns {@code false}.
     * </p>
     *
     * @return {@code true} to ignores the {@link com.github.wuic.nut.CompositeNut.CompositeInput}, {@code false} otherwise
     */
    boolean ignoreCompositeStreamOnTransformation();

    /**
     * <p>
     * Adds the given callback to be invoked once transformation occurs.
     * </p>
     *
     * @param onReady the callback to add
     * @param removeAfterInvocation {@code true} if the callback should be invoke one time and remove after transformation
     */
    void onReady(Pipe.OnReady onReady, boolean removeAfterInvocation);

    /**
     * <p>
     * Gets the callbacks notified when transformation has been done.
     * The map should never be {@code null} and must be immutable.
     * </p>
     *
     * @return the callbacks
     */
    List<Pipe.OnReady> getReadyCallbacks();

    /**
     * <p>
     * Adds a {@link Transformer}.
     * </p>
     *
     * @param transformer the transformer
     */
    void addTransformer(Transformer<ConvertibleNut> transformer);

    /**
     * <p>
     * Gets the registered transformers. The set must be ordered.
     * </p>
     *
     * @return the transformers
     */
    Set<Transformer<ConvertibleNut>> getTransformers();

    /**
     * <p>
     * Adds a new nut referenced by this nut.
     * </p>
     *
     * @param referenced the referenced nut
     */
    void addReferencedNut(ConvertibleNut referenced);

    /**
     * <p>
     * Gets the nuts referenced by this nut.
     * </p>
     *
     * @return the referenced nuts, {@code null} if no nut is referenced
     */
    List<ConvertibleNut> getReferencedNuts();

    /**
     * <p>
     * Indicates if this nut is transformed or not.
     * </p>
     *
     * @return {@code true} if transformed, {@code false} otherwise
     */
    boolean isTransformed();

    /**
     * <p>
     * Indicates if the nut is compressed.
     * </p>
     *
     * @param c {@code true} if the nut is compressed, {@code false} otherwise
     */
    void setIsCompressed(Boolean c);

    /**
     * <p>
     * Indicates if the nut is compressed.
     * </p>
     *
     * @return {@code true} if the nut is compressed, {@code false} otherwise
     */
    Boolean isCompressed();

    /**
     * <p>
     * Indicates if this nut is a sub resource of the nut referencing it. If this is {@code true}, then this means that
     * the referencer must loads this nut in order to work.
     * </p>
     *
     * @return {@code true} if the nut is a sub resource, {@code false} otherwise
     */
    boolean isSubResource();

    /**
     * <p>
     * Mark this nut as a sub resource or not.
     * </p>
     *
     * @param subResource {@code true} if the nut is a sub resource, {@code false} otherwise
     */
    void setIsSubResource(boolean subResource);

    /**
     * <p>
     * Adds a {@code BiFunction} invoked by the {@code Future} returned by {@link #getVersionNumber()} as soon as
     * the {@code Long} value is available in order to have a chance to modify it. This is handy when we want to
     * decorate the way version number is computed.
     * </p>
     *
     * <p>
     * The {@link BiFunction} will take as first parameter this {@code ConvertibleNut} and the version number as second
     * parameter. The functions has to return a {@code Long} representing the new version number. Multiple callback can
     * be added via this method to make a chain that will alter the version number.
     * </p>
     *
     * @param callback the function to invoke
     */
    void addVersionNumberCallback(BiFunction<ConvertibleNut, Long, Long> callback);

    /**
     * <p>
     * Returns an unmodifiable view of the {@link BiFunction callbacks} added via {@link #addVersionNumberCallback(BiFunction)}.
     * </p>
     *
     * @return the list, {@code null} if no callback has been registered
     */
    List<BiFunction<ConvertibleNut, Long, Long>> getVersionNumberCallbacks();
}
