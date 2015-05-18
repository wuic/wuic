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


package com.github.wuic.xml;

import com.github.wuic.ProcessContext;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.context.ContextBuilderConfigurator;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.exception.WuicException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.net.URL;

/**
 * <p>
 * This configurator implements XML supports for WUIC. It abstracts the way the XML is read and unmarshal with JAXB.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.4.0
 */
public abstract class XmlContextBuilderConfigurator extends ContextBuilderConfigurator {

    /**
     * To read wuic.xml content.
     */
    private Unmarshaller unmarshaller;

    /**
     * The process context.
     */
    private final ProcessContext processContext;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param processContext the process context
     * @throws JAXBException if an context can't be initialized
     */
    public XmlContextBuilderConfigurator(final ProcessContext processContext) throws JAXBException {
        this(Boolean.TRUE, processContext);
    }

    /**
     * <p>
     * Creates a new instance.
     * </p>
     *
     * @param pc the process context
     * @param multiple {@code true} if multiple configurations with the same tag could be executed, {@code false} otherwise
     * @throws JAXBException if an context can't be initialized
     */
    public XmlContextBuilderConfigurator(final Boolean multiple, final ProcessContext pc) throws JAXBException {
        super(multiple);
        final JAXBContext jc = JAXBContext.newInstance(XmlWuicBean.class);
        unmarshaller = jc.createUnmarshaller();
        processContext = pc == null ? ProcessContext.DEFAULT : pc;

        try {
            final URL xsd = getClass().getResource("/wuic.xsd");
            unmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(xsd));
        } catch (SAXException se) {
            WuicException.throwBadArgumentException(se);
        }
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
    public static void configureHeap(final ContextBuilder ctxBuilder, final XmlHeapBean heap) throws IOException {
        final String[] paths = heap.getNutPaths() == null ? new String[0] : heap.getNutPaths().toArray(new String[heap.getNutPaths().size()]);
        final String[] nested = configureNestedHeap(ctxBuilder, heap);
        final String[] referenced = getReferencedHeap(heap);

        // Merges referenced and nested heap into one array to give to context builder
        final String[] target = new String[(referenced == null ? 0 : referenced.length) + (nested == null ? 0 : nested.length)];

        // Nested exist
        if (nested != null) {
            System.arraycopy(nested, 0, target, 0, nested.length);
        }

        // Referenced exist
        if (referenced != null) {
            System.arraycopy(referenced, 0, target, nested == null ? 0 : nested.length, referenced.length);
        }

        // The heap is not a composition
        if (target.length == 0) {
            ctxBuilder.heap(heap.getId(), heap.getDaoBuilderId(), paths);
        } else {
            ctxBuilder.heap(false, heap.getId(), heap.getDaoBuilderId(), target, paths);
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
     * @param xml the bean
     * @param ctxBuilder the builder
     * @throws WorkflowTemplateNotFoundException if a workflow-template-id reference a non existing template
     * @throws IOException if any I/O error occurs
     */
    public static void configureTemplates(final XmlWuicBean xml, final ContextBuilder ctxBuilder)
            throws WorkflowTemplateNotFoundException, IOException {
        if (xml.getWorkflowTemplates() == null) {
            return;
        }

        // Create each template
        for (final XmlWorkflowTemplateBean template : xml.getWorkflowTemplates()) {

            // DAO where we can store process result is optional
            if (template.getDaoBuilderIds() == null) {
                ctxBuilder.template(template.getId(),
                        template.getEngineBuilderIds().toArray(new String[template.getEngineBuilderIds().size()]),
                        template.getWithoutEngineBuilderIds() == null ?
                                null : template.getWithoutEngineBuilderIds().toArray(new String[template.getWithoutEngineBuilderIds().size()]),
                        template.getUseDefaultEngines());
            } else {
                ctxBuilder.template(template.getId(),
                        template.getEngineBuilderIds().toArray(new String[template.getEngineBuilderIds().size()]),
                        template.getWithoutEngineBuilderIds() == null ?
                                null : template.getWithoutEngineBuilderIds().toArray(new String[template.getWithoutEngineBuilderIds().size()]),
                        template.getUseDefaultEngines(),
                        template.getDaoBuilderIds().toArray(new String[template.getDaoBuilderIds().size()]));
            }
        }
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
     * @param xml the bean
     * @param ctxBuilder the builder
     * @throws com.github.wuic.exception.WorkflowTemplateNotFoundException if a workflow-template-id reference a non existing template
     * @throws IOException if any I/O error occurs
     */
    public static void configureWorkflow(final XmlWuicBean xml, final ContextBuilder ctxBuilder)
            throws WorkflowTemplateNotFoundException, IOException {
        if (xml.getWorkflows() == null) {
            return;
        }

        // Some additional DAOs where process result is saved
        for (final XmlWorkflowBean workflow : xml.getWorkflows()) {
            if (!(workflow.getId() == null && workflow.getIdPrefix() != null
                    || workflow.getId() != null && workflow.getIdPrefix() == null)) {
                WuicException.throwWuicXmlWorkflowIdentifierException(workflow.getIdPrefix(), workflow.getId());
            }

            final Boolean forEachHeap = workflow.getId() == null;

            // DAO where we can store process result is optional
            ctxBuilder.workflow(forEachHeap ? workflow.getIdPrefix() : workflow.getId(),
                    forEachHeap,
                    workflow.getHeapIdPattern(),
                    workflow.getWorkflowTemplateId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessContext getProcessContext() {
        return processContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int internalConfigure(final ContextBuilder ctxBuilder) {
        try {
            // Let's load the wuic.xml file and configure the builder with it
            final XmlWuicBean xml = unmarshal(unmarshaller);

            // The filters
            if (xml.getFilterBuilders() != null) {
                for (final XmlBuilderBean filter : xml.getFilterBuilders()) {
                    extractProperties(filter, ctxBuilder.contextNutFilterBuilder(filter.getId(), filter.getType())).toContext();
                }
            }

            // The DAOs
            if (xml.getDaoBuilders() != null) {
                for (final XmlBuilderBean dao : xml.getDaoBuilders()) {
                    extractProperties(dao, ctxBuilder.contextNutDaoBuilder(dao.getId(), dao.getType())).toContext();
                }
            }

            // The heaps
            if (xml.getHeaps() != null) {
                for (final XmlHeapBean heap : xml.getHeaps()) {
                    configureHeap(ctxBuilder, heap);
                }
            }

            // The engines
            if (xml.getEngineBuilders() != null) {
                for (final XmlBuilderBean engine : xml.getEngineBuilders()) {
                    extractProperties(engine, ctxBuilder.contextEngineBuilder(engine.getId(), engine.getType())).toContext();
                }
            }

            configureTemplates(xml, ctxBuilder);
            configureWorkflow(xml, ctxBuilder);

            return xml.getPollingIntervalSeconds();
        } catch (JAXBException je) {
            WuicException.throwBadArgumentException(je);
        } catch (IOException se) {
            WuicException.throwBadArgumentException(se);
        } catch (WorkflowTemplateNotFoundException wxwtnfe) {
            WuicException.throwBadArgumentException(wxwtnfe);
        }

        return 0;
    }

    /**
     * <p>
     * Gets nested declaration of a heap inside the given heap and configure the given context builder with them.
     * </p>
     *
     * @param ctxBuilder the context builder
     * @param heap the enclosing heap
     * @return the extracted heaps
     * @throws IOException if an I/O error occurs
     */
    private static String[] configureNestedHeap(final ContextBuilder ctxBuilder, final XmlHeapBean heap) throws IOException {
        if (heap.getNestedComposition() == null || heap.getNestedComposition().isEmpty()) {
            return null;
        } else {
            final String[] retval = new String[heap.getNestedComposition().size()];

            for (int i = 0; i < heap.getNestedComposition().size(); i++) {
                final XmlHeapBean nested = heap.getNestedComposition().get(i);
                final String[] paths = nested.getNutPaths() == null ? new String[0] : nested.getNutPaths().toArray(new String[nested.getNutPaths().size()]);
                ctxBuilder.heap(nested.getId(), nested.getDaoBuilderId(), paths);
                retval[i] = nested.getId();
            }

            return retval;
        }
    }

    /**
     * <p>
     * Gets referenced declaration of a heap inside the given heap.
     * </p>
     *
     * @param heap the enclosing heap
     * @return the extracted heaps
     */
    private static String[] getReferencedHeap(final XmlHeapBean heap) {
        if (heap.getReferencedComposition() == null || heap.getReferencedComposition().isEmpty()) {
            return null;
        } else {
            final String[] retval = new String[heap.getReferencedComposition().size()];

            for (int i = 0; i < heap.getReferencedComposition().size(); i++) {
                retval[i] = heap.getReferencedComposition().get(i);
            }

            return retval;
        }
    }

    /**
     * <p>
     * Extracts from the given bean some properties to inject to the specified builder.
     * </p>
     *
     * @param bean the bean
     * @param contextGenericBuilder the context generic builder
     * @return the {@link ContextBuilder.ContextGenericBuilder} associating each property ID to its value
     */
    private ContextBuilder.ContextGenericBuilder extractProperties(final XmlBuilderBean bean,
                                                                   final ContextBuilder.ContextGenericBuilder contextGenericBuilder) {
        if (bean.getProperties() == null) {
            return contextGenericBuilder;
        } else {
            for (final XmlPropertyBean propertyBean : bean.getProperties()) {
                contextGenericBuilder.property(propertyBean.getKey(), propertyBean.getValue());
            }
        }

        return contextGenericBuilder;
    }

    /**
     * <p>
     * Unmashal the {@link XmlWuicBean} with the given unmarhalled.
     * </p>
     *
     * @param unmarshaller the unmarshaller
     * @return the unmarshalled bean
     * @throws JAXBException if the XML can't be read
     */
    protected abstract XmlWuicBean unmarshal(final Unmarshaller unmarshaller) throws JAXBException;
}
