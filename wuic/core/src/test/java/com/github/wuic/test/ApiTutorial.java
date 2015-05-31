/*
 * "Copyright (c) 2015   Capgemini Technology Services (final hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (final the "Software"), to use, copy, modify and
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
 * open source software licenses (final BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic.test;

import com.github.wuic.ProcessContext;
import com.github.wuic.WuicFacade;
import com.github.wuic.WuicFacadeBuilder;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.engine.core.TextAggregatorEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.core.DiskNutDao;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * Pieces of codes explained in Java Config documentation.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.2
 */
public class ApiTutorial {

    final Logger log = LoggerFactory.getLogger(getClass());

    void create() {
        // tag::CreateFacadeAPI[]
        try {
            final WuicFacade facade = new WuicFacadeBuilder().contextPath("/statics").build();
        } catch (WuicException e) {
            log.error("Unable to build the facade.", e);
        }
        // end::CreateFacadeAPI[]

        // tag::CreateFacadeNoDefaultAPI[]
        try {
            final WuicFacade facade = new WuicFacadeBuilder()
                    .noDefaultContextBuilderConfigurator()
                    .build();
        } catch (WuicException e) {
            log.error("Unable to build the facade.", e);
        }
        // end::CreateFacadeNoDefaultAPI[]

        // tag::CreateFacadeContextAPI[]
        final WuicFacadeBuilder builder =  new WuicFacadeBuilder();
        final String engineId = ContextBuilder.getDefaultBuilderId(TextAggregatorEngine.class);
        final String daoId = ContextBuilder.getDefaultBuilderId(DiskNutDao.class);

        try {
            final WuicFacade facade = builder.noDefaultContextBuilderConfigurator()
                    .contextBuilder()
                    // tag the configuration, always release the tag in a finally block to not face deadlock issue
                    .tag("custom")
                    // create a ContextNutDaoBuilder which produces DAO of type DiskNutDao
                    .contextNutDaoBuilder(DiskNutDao.class)
                    // override default property by telling that any path specified is relative to /static
                    .property("c.g.wuic.dao.basePath", "/statics")
                    // back to ContextBuilder
                    .toContext()
                    // create a heap using "daoId" with two nuts "darth.js" and "vader.js"
                    .heap("heap", daoId, new String[] { "dark.js", "vador.js"} )
                    // create a ContextEngineBuilder which produces engine of type TextAggregatorEngine
                    .contextEngineBuilder(TextAggregatorEngine.class)
                    // back to ContextBuilder
                    .toContext()
                    // creates a template "tpl" that describes a process that call the engine
                    .template("tpl", new String[]{ engineId })
                    // creates a workflow "starwarsWorkflow" with heap "heap" and the process described by "tpl"
                    .workflow("starwarsWorkflow", true, "heap", "tpl")
                    // release the tag before build
                    .releaseTag()
                    // back to FacadeBuilder
                    .toFacade()
                    // Build facade
                    .build();
        } catch (IOException ioe) {
            log.error("Unable to build the facade.", ioe);
        } catch (WuicException e) {
            log.error("Unable to build the facade.", e);
        } finally {
            builder.contextBuilder().releaseTag();
        }
        // end::CreateFacadeContextAPI[]

        // tag::CreateFacadeWithConfiguratorAPI[]
        try {
            final WuicFacade facade = new WuicFacadeBuilder().contextBuilderConfigurators(new MyContextBuilderConfigurator()).build();
        } catch (WuicException e) {
            log.error("Unable to build the facade.", e);
        }
        // end::CreateFacadeWithConfiguratorAPI[]

        final WuicFacade facade = Mockito.mock(WuicFacade.class);

        // tag::ConfigureAPI[]
        try {
            facade.configure(new MyContextBuilderConfigurator());
        } catch (WuicException e) {
            log.error("Unable to build the facade.", e);
        }
        // end::ConfigureAPI[]

        // tag::RunWorkflowAPI[]
        try {
            // Contains one nut
            final List<ConvertibleNut> nut = facade.runWorkflow("starwarsWorkflow", ProcessContext.DEFAULT);

            nut.get(0).getName();    // aggregate.js
            nut.get(0).openStream(); // InputStream
        } catch (WuicException e) {
            log.error("Unable to build the facade.", e);
        } catch (IOException ioe) {
            log.error("Unable to build the facade.", ioe);
        }
        // end::RunWorkflowAPI[]
    }

    // tag::CustomConfiguratorAPI[]
    public class MyContextBuilderConfigurator extends ContextBuilderConfigurator {

        /**
         * {@inheritDoc}
         */
        @Override
        public int internalConfigure(final ContextBuilder ctxBuilder) {
            // ...
            return -1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getTag() {
            return "myCfg";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ProcessContext getProcessContext() {
            return ProcessContext.DEFAULT;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Long getLastUpdateTimestampFor(final String path) throws IOException {
            return -1L;
        }
    }
    // end::CustomConfiguratorAPI[]
}
