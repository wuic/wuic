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


package com.github.wuic.config.bean.json;

import com.github.wuic.ProcessContext;
import com.github.wuic.config.bean.BuilderBean;
import com.github.wuic.config.bean.HeapBean;
import com.github.wuic.config.bean.HeapReference;
import com.github.wuic.config.bean.PropertyBean;
import com.github.wuic.config.bean.WorkflowBean;
import com.github.wuic.config.bean.WorkflowTemplateBean;
import com.github.wuic.config.bean.WuicBean;
import com.github.wuic.context.AbstractContextBuilderConfigurator;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.exception.WuicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class relies on a {@link com.github.wuic.config.bean.WuicBean} to perform context configuration.
 * The way the bean is retrieved is abstract and must be implemented by sub class.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class BeanContextBuilderConfigurator extends AbstractContextBuilderConfigurator {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BeanContextBuilderConfigurator.class);

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param tag the tag
     * @param pc the process context
     * @param multiple {@code true} if multiple configurations with the same tag could be executed, {@code false} otherwise
     */
    public BeanContextBuilderConfigurator(final Boolean multiple, final String tag, final ProcessContext pc) {
        super(multiple, tag, pc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int internalConfigure(final ContextBuilder ctxBuilder) {
        try {
            // Let's load the configuration file and configure the builder with it
            final WuicBean bean = getWuicBean();

            // The filters
            if (bean.getFilterBuilders() != null) {
                for (final BuilderBean filter : bean.getFilterBuilders()) {
                    ctxBuilder.addChildrenTag(getTag(), tagProfiles(filter, filter.getProfiles(), ctxBuilder));
                    extractProperties(filter, ctxBuilder, ctxBuilder.contextNutFilterBuilder(filter.getId(), filter.getType())).toContext();

                    // Back to configurator tag
                    tag(ctxBuilder);
                }
            }

            // The DAOs
            if (bean.getDaoBuilders() != null) {
                for (final BuilderBean dao : bean.getDaoBuilders()) {
                    ctxBuilder.addChildrenTag(getTag(), tagProfiles(dao, dao.getProfiles(), ctxBuilder));
                    extractProperties(dao, ctxBuilder, ctxBuilder.contextNutDaoBuilder(dao.getId(), dao.getType())).toContext();

                    // Back to configurator tag
                    tag(ctxBuilder);
                }
            }

            // The heaps
            if (bean.getHeaps() != null) {
                for (final HeapBean heap : bean.getHeaps()) {
                    ctxBuilder.addChildrenTag(getTag(), tagProfiles(heap, heap.getProfiles(), ctxBuilder));
                    configureHeap(ctxBuilder, heap);

                    // Back to configurator tag
                    tag(ctxBuilder);
                }
            }

            // The engines
            if (bean.getEngineBuilders() != null) {
                for (final BuilderBean engine : bean.getEngineBuilders()) {
                    ctxBuilder.addChildrenTag(getTag(), tagProfiles(engine, engine.getProfiles(), ctxBuilder));
                    extractProperties(engine, ctxBuilder, ctxBuilder.contextEngineBuilder(engine.getId(), engine.getType())).toContext();

                    // Back to configurator tag
                    tag(ctxBuilder);
                }
            }

            // configure templates and workflow
            final List<Object> templateTags = configureTemplates(bean, ctxBuilder);
            tag(ctxBuilder);
            ctxBuilder.addChildrenTag(getTag(), templateTags);
            ctxBuilder.addChildrenTag(getTag(), configureWorkflow(bean, ctxBuilder));

            return bean.getPollingIntervalSeconds();
        } catch (WuicException we) {
            WuicException.throwBadArgumentException(we);
        } catch (IOException se) {
            WuicException.throwBadArgumentException(se);
        }

        return 0;
    }


    /**
     * <p>
     * Configures the heap described in the given bean.
     * </p>
     *
     * @param ctxBuilder the builder to configure
     * @param heap the configuration bean
     * @throws IOException if any I/O error occurs
     */
    public static void configureHeap(final ContextBuilder ctxBuilder, final HeapBean heap) throws IOException {
        final List<String> paths = new ArrayList<String>();
        final List<String> heaps = new ArrayList<String>();
        collectElements(ctxBuilder, heap, paths, heaps);

        final String[] pathArray = paths.toArray(new String[paths.size()]);

        // The heap is not a composition
        if (heaps.isEmpty()) {
            ctxBuilder.heap(heap.getId(), heap.getDaoBuilderId(), pathArray);
        } else {
            ctxBuilder.heap(false, heap.getId(), heap.getDaoBuilderId(), heaps.toArray(new String[heaps.size()]), pathArray);
        }
    }

    /**
     * <p>
     * Configures the given builder with the template in the specified bean.
     * </p>
     *
     * <p>
     * An {@link IllegalArgumentException} is thrown if the workflow is badly defined.
     * </p>
     *
     * @param bean the bean
     * @param ctxBuilder the builder
     * @throws com.github.wuic.exception.WorkflowTemplateNotFoundException if a workflow-template-id reference a non existing template
     * @throws IOException if any I/O error occurs
     * @return tags created by this method related to settings restricted by specific profiles
     */
    public static List<Object> configureTemplates(final WuicBean bean, final ContextBuilder ctxBuilder)
            throws WorkflowTemplateNotFoundException, IOException {
        final List<Object> childrenTags = new ArrayList<Object>();

        if (bean.getWorkflowTemplates() == null) {
            return childrenTags;
        }

        // Create each template
        for (final WorkflowTemplateBean template : bean.getWorkflowTemplates()) {
            final Object childTag = tagProfiles(template, template.getProfiles(), ctxBuilder);

            if (childTag != null) {
                childrenTags.add(childTag);
            }

            ctxBuilder.template(template.getId(),
                    template.getEngineBuilderIds().toArray(new String[template.getEngineBuilderIds().size()]),
                    template.getWithoutEngineBuilderType() == null ?
                            null : template.getWithoutEngineBuilderType().toArray(new String[template.getWithoutEngineBuilderType().size()]),
                    template.getUseDefaultEngines());
        }

        return childrenTags;
    }

    /**
     * <p>
     * Configures the given builder with the specified bean.
     * </p>
     *
     * <p>
     * An {@link IllegalArgumentException} is thrown if the workflow is badly defined.
     * </p>
     *
     * @param bean the bean
     * @param ctxBuilder the builder
     * @return tags created by this method related to settings restricted by specific profiles
     * @throws com.github.wuic.exception.WorkflowTemplateNotFoundException if a workflow-template-id reference a non existing template
     * @throws IOException if any I/O error occurs
     */
    public static List<Object> configureWorkflow(final WuicBean bean, final ContextBuilder ctxBuilder)
            throws WorkflowTemplateNotFoundException, IOException {
        final List<Object> childrenTags = new ArrayList<Object>();

        if (bean.getWorkflows() == null) {
            return childrenTags;
        }

        // Some additional DAOs where process result is saved
        for (final WorkflowBean workflow : bean.getWorkflows()) {
            if (!(workflow.getId() == null && workflow.getIdPrefix() != null
                    || workflow.getId() != null && workflow.getIdPrefix() == null)) {
                WuicException.throwWuicXmlWorkflowIdentifierException(workflow.getIdPrefix(), workflow.getId());
            }

            final Object childTag = tagProfiles(workflow, workflow.getProfiles(), ctxBuilder);

            if (childTag != null) {
                childrenTags.add(childTag);
            }

            final Boolean forEachHeap = workflow.getId() == null;

            // DAO where we can store process result is optional
            ctxBuilder.workflow(forEachHeap ? workflow.getIdPrefix() : workflow.getId(),
                    forEachHeap,
                    workflow.getHeapIdPattern(),
                    workflow.getWorkflowTemplateId());
        }

        return childrenTags;
    }

    /**
     * <p>
     * Collects different elements that compose the heap specified in parameter.
     * </p>
     *
     * Supported operations are:
     * <ul>
     *  <li>Gets nested declaration of a heap inside the given heap and configure the given context builder with them.</li>
     *  <li>Gets referenced declaration of a heap inside the given heap.</li>
     *  <li>Gets all paths that represent the nuts.</li>
     * </ul>
     *
     * @param ctxBuilder the context builder
     * @param heap the enclosing heap
     * @param paths the paths that compose the heap
     * @param heaps the collected heaps
     * @throws IOException if an I/O error occurs
     */
    private static void collectElements(final ContextBuilder ctxBuilder,
                                        final HeapBean heap,
                                        final List<String> paths,
                                        final List<String> heaps) throws IOException {
        if (heap.getElements() != null) {
            for (final Object element : heap.getElements()) {
                if (element instanceof HeapBean)  {
                    final HeapBean nested = HeapBean.class.cast(element);

                    if (nested.getElements() == null) {
                        continue;
                    }

                    final List<String> nestedPaths = new ArrayList<String>();

                    for (final Object nestedPath : nested.getElements()) {
                        if (nestedPath instanceof String) {
                            nestedPaths.add(String.valueOf(nestedPath));
                        } else {
                            LOGGER.warn("Nested heap {} should only declare nut-path and avoid heap declaration.", nested.getId());
                        }
                    }

                    ctxBuilder.heap(nested.getId(), nested.getDaoBuilderId(), nestedPaths.toArray(new String[nestedPaths.size()]));
                    heaps.add(nested.getId());
                } else if (element instanceof HeapReference) {
                    heaps.add(HeapReference.class.cast(element).getValue());
                } else {
                    paths.add(element.toString());
                }
            }
        }
    }

    /**
     * <p>
     * Checks if the given bean contains any profile to declare and in that case apply a new tag.
     * </p>
     *
     * @param bean the bean
     * @param beanProfiles the bean's profiles
     * @param contextBuilder the builder to configure
     * @return the tag associated to the profiles, {@code null} if no tag has been applied
     */
    public static Object tagProfiles(final Object bean,
                                     final String[] beanProfiles,
                                     final ContextBuilder contextBuilder) {
        if (beanProfiles != null) {
            contextBuilder.tag(bean.toString(), beanProfiles);
            return bean.toString();
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Extracts from the given bean some properties to inject to the specified builder.
     * </p>
     *
     * @param bean the bean
     * @param contextBuilder the context builder
     * @param contextGenericBuilder the context generic builder
     * @return the {@link ContextBuilder.ContextGenericBuilder} associating each property ID to its value
     */
    public static ContextBuilder.ContextGenericBuilder extractProperties(final BuilderBean bean,
                                                                         final ContextBuilder contextBuilder,
                                                                         final ContextBuilder.ContextGenericBuilder contextGenericBuilder) {
        if (bean.getProperties() != null) {
            for (final PropertyBean propertyBean : bean.getProperties()) {
                final String value = contextBuilder.injectPlaceholder(propertyBean.getValue());

                // Apply component default value instead
                if (value != null) {
                    contextGenericBuilder.property(propertyBean.getKey(), value);
                }
            }
        }

        return contextGenericBuilder;
    }

    /**
     * <p>
     * Gets the configuration bean used by this configurator.
     * </p>
     *
     * @return the bean
     * @throws WuicException if bean can't be retrieved
     */
    public abstract WuicBean getWuicBean() throws WuicException;
}
