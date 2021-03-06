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

import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.config.ObjectBuilderInspector;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.BiFunction;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * This {@code ObjectBuilderFactory} is a specialized class to produce {@code Engine} object. When a {@link NodeEngine}
 * is produced, the {@link NodeEngine#setVersionNumberCallback(com.github.wuic.util.BiFunction)} is called in order to
 * register a function that will modify the version number according to the {@code hashCode()} value of all key/value
 * pairs provided by the builder's properties.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class EngineObjectBuilderFactory extends ObjectBuilderFactory<Engine> {

    /**
     * <p>
     * Builds a new instance based on the given {@code ObjectBuilderFactory}.
     * </p>
     *
     * @param objectBuilderFactory the {@code ObjectBuilderFactory} to copy
     */
    public EngineObjectBuilderFactory(final ObjectBuilderFactory<Engine> objectBuilderFactory) {
        super(objectBuilderFactory);
    }

    /**
     * <p>
     * Builds a default new instance.
     * </p>
     */
    public EngineObjectBuilderFactory() {
        super(EngineService.class, EngineService.DEFAULT_SCAN_PACKAGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Engine inspect(final ObjectBuilder<Engine> builder, final Map<Class, List<ObjectBuilderInspector>> inspectors, final Engine object) {
        if (object instanceof NodeEngine) {

            // Register the callback to the NodeEngine
            NodeEngine.class.cast(object).setVersionNumberCallback(new BiFunction<ConvertibleNut, Long, Long>() {
                @Override
                public Long apply(final ConvertibleNut convertibleNut, final Long versionNumber) {
                    Long retval = versionNumber;

                    // Alter the version number according to the builder's properties
                    for (final Map.Entry<String, Object>  prop : builder.getProperties().entrySet()) {
                        final String value = String.valueOf(prop.getValue());
                        retval += String.format("%s:%s=%s", builder.getType().getName(), prop.getKey(), value).hashCode();
                    }

                    return retval;
                }
            });
        }

        return super.inspect(builder, inspectors, object);
    }
}
