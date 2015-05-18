package com.github.wuic.test.engine;

import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineRequestBuilder;
import com.github.wuic.engine.core.AbstractAggregatorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutsHeap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This class tests {@link AbstractAggregatorEngine}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.2
 */
@RunWith(JUnit4.class)
public class AbstractAggregatorEngineTest {

    /**
     * <p>
     * This implementation just returns a mocked nut when aggregation is performed.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.5.2
     */
    final class A extends AbstractAggregatorEngine {

        /**
         * <p>
         * Builds a new instance.
         * </p>
         */
        A() {
            super(Boolean.TRUE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<ConvertibleNut> aggregationParse(final EngineRequest request) throws WuicException {
            return Arrays.asList(Mockito.mock(ConvertibleNut.class));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<NutType> getNutTypes() {
            return Arrays.asList(NutType.values());
        }
    }

    /**
     * Checks that a dynamic nut is never aggregated.
     *
     * @throws WuicException if test fails
     */
    @Test
    public void dynamicNutTest() throws WuicException {
        final Engine e = new A();
        final List<Nut> nuts = new ArrayList<Nut>();

        for (int i = 0; i < 6; i++) {
            final Nut nut = Mockito.mock(Nut.class);
            Mockito.when(nut.getInitialNutType()).thenReturn(NutType.JAVASCRIPT);
            Mockito.when(nut.getInitialName()).thenReturn(i + ".js");
            Mockito.when(nut.isDynamic()).thenReturn(i % 2 == 0);
            nuts.add(nut);
        }

        final NutsHeap h = Mockito.mock(NutsHeap.class);
        Mockito.when(h.getNuts()).thenReturn(nuts);

        final List<ConvertibleNut> res = e.parse(new EngineRequestBuilder("", h, null).processContext(ProcessContext.DEFAULT).build());
        Assert.assertEquals(4, res.size());
    }
}
