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


package com.github.wuic.context;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.Workflow;
import com.github.wuic.WorkflowTemplate;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * <p>
 * This class wraps a map associating arbitrary tag objects to their {@link ContextSetting}. On top of this map, a lot
 * of utility methods are provided to help the {@link ContextBuilder} when building the {@link Context}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.5.1
 */
public class TaggedSettings {

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * All settings associated to their tag.
     */
    private final Map<Object, ContextSetting> taggedSettings;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     */
    TaggedSettings() {
        taggedSettings = new HashMap<Object, ContextSetting>();
    }

    /**
     * <p>
     * Puts a new setting for the given tag.
     * </p>
     *
     * @param tag the tag
     * @param setting the setting
     */
    void put(final Object tag, final ContextSetting setting) {
        taggedSettings.put(tag, setting);
    }

    /**
     * <p>
     * Gets the setting associated to the given tag.
     * </p>
     *
     * @param tag the tag
     * @return the setting
     */
    ContextSetting get(final Object tag) {
        return taggedSettings.get(tag);
    }

    /**
     * <p>
     * Removes the setting associated to the given tag.
     * </p>
     *
     * @param tag the tag
     * @return the removed setting
     */
    ContextSetting remove(final Object tag) {
        return taggedSettings.remove(tag);
    }

    /**
     * <p>
     * Gets the {@link NutDao} registration associated to the given ID.
     * </p>
     *
     * @param id the ID
     * @return the registration, {@code null} if no registration has been found
     */
    ContextBuilder.NutDaoRegistration getNutDaoRegistration(final String id) {
        for (final ContextSetting setting : taggedSettings.values()) {
            final ContextBuilder.NutDaoRegistration registration = setting.getNutDaoMap().get(id);

            if (registration != null) {
                return registration;
            }
        }

        return null;
    }

    /**
     * <p>
     * Removes the {@link NutDao} registration associated to the given ID. The removed
     * {@link ContextBuilder.NutDaoRegistration} is {@link ContextBuilder.NutDaoRegistration#free()}.
     * </p>
     *
     * @param id the ID
     */
    void removeNutDaoRegistration(final String id) {
        for (final ContextSetting s : taggedSettings.values()) {
            final ContextBuilder.NutDaoRegistration n = s.getNutDaoMap().remove(id);

            if (n != null) {
                n.free();
            }
        }
    }

    /**
     * <p>
     * Gets the {@link Workflow} registration associated to the given ID.
     * </p>
     *
     * @param id the ID
     */
    void removeWorkflowRegistration(final String id) {
        for (final ContextSetting s : taggedSettings.values()) {
            s.getWorkflowMap().remove(id);
        }
    }

    /**
     * <p>
     * Gets the {@link NutsHeap} registration associated to the given ID. The removed entry is
     * {@link com.github.wuic.context.ContextBuilder.HeapRegistration#free()}.
     * </p>
     *
     * @param id the ID
     */
    void removeHeapRegistration(final String id) {
        for (final ContextSetting s : taggedSettings.values()) {
            final ContextBuilder.HeapRegistration h = s.getNutsHeaps().remove(id);

            if (h != null) {
                h.free();
            }
        }
    }

    /**
     * <p>
     * Gets the {@link NutFilter} registration associated to the given ID.
     * </p>
     *
     * @param id the ID
     */
    void removeNutFilter(final String id) {
        for (final ContextSetting s : taggedSettings.values()) {
            s.getNutFilterMap().remove(id);
        }
    }

    /**
     * <p>
     * Gets the {@link Engine} registration associated to the given ID.
     * </p>
     *
     * @param id the ID
     */
    void removeEngine(final String id) {
        for (final ContextSetting s : taggedSettings.values()) {
            s.getEngineMap().remove(id);
        }
    }

    /**
     * <p>
     * Merges all the specified settings to the given builder.
     * </p>
     *
     * @param contextBuilder the target builder
     * @param other the source
     * @param currentTag the current builder's tag
     */
    void mergeSettings(final ContextBuilder contextBuilder, final TaggedSettings other, final Object currentTag) {
        for (final ContextSetting s : other.taggedSettings.values()) {
            contextBuilder.processContext(s.getProcessContext());

            for (final Map.Entry<String, ContextBuilder.NutDaoRegistration> entry : s.getNutDaoMap().entrySet()) {
                contextBuilder.nutDao(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<String, ObjectBuilder<Engine>> entry : s.getEngineMap().entrySet()) {
                contextBuilder.engineBuilder(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<String, ObjectBuilder<NutFilter>> entry : s.getNutFilterMap().entrySet()) {
                contextBuilder.nutFilter(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<String, ContextBuilder.HeapRegistration> entry : s.getNutsHeaps().entrySet()) {
                s.getNutsHeaps().remove(entry.getKey());
                final ContextSetting setting = contextBuilder.getSetting();
                setting.getNutsHeaps().put(entry.getKey(), entry.getValue());
                taggedSettings.put(currentTag, setting);
            }

            for (final Map.Entry<String, ContextBuilder.WorkflowTemplateRegistration> entry : s.getTemplateMap().entrySet()) {
                s.getTemplateMap().remove(entry.getKey());
                final ContextSetting setting = contextBuilder.getSetting();
                setting.getTemplateMap().put(entry.getKey(), entry.getValue());
                taggedSettings.put(currentTag, setting);
            }

            for (final Map.Entry<String, ContextBuilder.WorkflowRegistration> entry : s.getWorkflowMap().entrySet()) {
                s.getWorkflowMap().remove(entry.getKey());
                final ContextSetting setting = contextBuilder.getSetting();
                setting.getWorkflowMap().put(entry.getKey(), entry.getValue());
                taggedSettings.put(currentTag, setting);
            }

            final ContextSetting setting = contextBuilder.getSetting();
            setting.getInterceptorsList().addAll(s.getInterceptorsList());
            taggedSettings.put(currentTag, setting);
        }
    }

    /**
     * <p>
     * Gets the {@link NutFilter filters} associated to their ID currently configured in all settings.
     * </p>
     *
     * @return the filters
     */
    Map<String, NutFilter> getFilterMap() {
        final Map<String, NutFilter> retval = new HashMap<String, NutFilter>();

        for (final ContextSetting setting : taggedSettings.values()) {
            for (final Map.Entry<String, ObjectBuilder<NutFilter>> objectBuilder : setting.getNutFilterMap().entrySet()) {
                retval.put(objectBuilder.getKey(), objectBuilder.getValue().build());
            }
        }

        return retval;
    }

    /**
     * <p>
     * Gets the {@link ContextInterceptor inspectors} currently configured in all settings.
     * </p>
     *
     * @return the inspectors
     */
    List<ContextInterceptor> getInspectors() {
        final List<ContextInterceptor> interceptors = new ArrayList<ContextInterceptor>();

        for (final ContextSetting setting : taggedSettings.values()) {
            interceptors.addAll(setting.getInterceptorsList());
        }

        return interceptors;
    }


    /**
     * <p>
     * Gets the {@link com.github.wuic.context.ContextBuilder.HeapRegistration registration} associated to an ID matching
     * the given regex.
     * </p>
     *
     * @param regex the regex ID
     * @return the matching {@link com.github.wuic.context.ContextBuilder.HeapRegistration registration}
     */
    Map<String, ContextBuilder.HeapRegistration> getNutsHeap(final String regex) {
        final Map<String, ContextBuilder.HeapRegistration> retval = new HashMap<String, ContextBuilder.HeapRegistration>();
        final Pattern pattern = Pattern.compile(regex);

        for (final ContextSetting setting : taggedSettings.values()) {
            for (final Map.Entry<String, ContextBuilder.HeapRegistration> entry : setting.getNutsHeaps().entrySet()) {
                if (pattern.matcher(entry.getKey()).matches()) {
                    retval.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return retval;
    }
    
    /**
     * <p>
     * Gets the {@link WorkflowTemplate} associated to the given ID.
     * </p>
     *
     * @param id the ID
     * @param daoCollection the collection of declared {@link NutDao}
     * @return the matching {@link WorkflowTemplate template}
     */
    WorkflowTemplate getWorkflowTemplate(final String id, final Map<String, NutDao> daoCollection) {
        final Iterator<ContextSetting> it = taggedSettings.values().iterator();
        ContextBuilder.WorkflowTemplateRegistration retval = null;

        while (it.hasNext() && retval == null) {
            retval = it.next().getTemplateMap().get(id);
        }

        return retval != null ? retval.getTemplate(daoCollection) : null;
    }

    /**
     * <p>
     * Gets all the {@link NutDao} created with the existing registrations.
     * </p>
     *
     * @return the {@link NutDao} associated to their registration ID
     */
    Map<String, NutDao> getNutDaoMap() {
        final Map<String, NutDao> daoMap = new HashMap<String, NutDao>();
        final Map<ContextBuilder.NutDaoRegistration, List<String>> registrationMap =
                new HashMap<ContextBuilder.NutDaoRegistration, List<String>>();

        // Organize all registrations grouped by associated keys in order instantiate each registration only once
        for (final ContextSetting setting : taggedSettings.values()) {
            for (final Map.Entry<String, ContextBuilder.NutDaoRegistration> dao : setting.getNutDaoMap().entrySet()) {
                List<String> keys = registrationMap.get(dao.getValue());

                if (keys == null) {
                    keys = new ArrayList<String>();
                    registrationMap.put(dao.getValue(), keys);
                }

                keys.add(dao.getKey());
            }
        }

        // Populate the DAO map with the same instance associated to all keys sharing the same registration
        final List<ProxyNutDaoRegistration> proxyRegistrations = new ArrayList<ProxyNutDaoRegistration>();
        for (final Map.Entry<ContextBuilder.NutDaoRegistration, List<String>> registration : registrationMap.entrySet()) {
            final NutDao dao = registration.getKey().getNutDao(proxyRegistrations);

            for (final String key : registration.getValue()) {
                daoMap.put(key, dao);
            }
        }

        // Some DAO are proxy with rules related to other DAO, set those DAO
        for (final ProxyNutDaoRegistration proxyNutDaoRegistration : proxyRegistrations) {
            proxyNutDaoRegistration.addRule(daoMap.get(proxyNutDaoRegistration.getDaoId()));
        }

        return daoMap;
    }

    /**
     * <p>
     * Gets all the {@link NutsHeap} created with the existing registrations.
     * </p>
     *
     * @return the {@link NutsHeap} associated to their registration ID
     */
    Map<String, NutsHeap> getNutsHeapMap(final Map<String, NutDao> daoMap, final Map<String, NutFilter> nutFilterMap)
            throws IOException {
        final Map<String, NutsHeap> heapMap = new HashMap<String, NutsHeap>();

        // The composite heap must be read after the standard heap
        for (final ContextSetting setting : taggedSettings.values()) {
            for (final Map.Entry<String, ContextBuilder.HeapRegistration> heap : setting.getNutsHeaps().entrySet()) {
                if (!heap.getValue().isComposition()) {
                    heapMap.put(heap.getKey(), heap.getValue().getHeap(heap.getKey(), daoMap, heapMap, nutFilterMap, setting));
                }
            }
        }

        // Now read all compositions
        for (final ContextSetting setting : taggedSettings.values()) {
            for (final Map.Entry<String, ContextBuilder.HeapRegistration> heap : setting.getNutsHeaps().entrySet()) {
                if (heap.getValue().isComposition()) {
                    heapMap.put(heap.getKey(), heap.getValue().getHeap(heap.getKey(), daoMap, heapMap, nutFilterMap, setting));
                }
            }
        }

        return heapMap;
    }

    /**
     * <p>
     * Gets all the {@link Workflow} created with the existing registrations.
     * </p>
     *
     * @return the {@link Workflow} associated to their registration ID
     */
    Map<String, Workflow> getWorkflowMap(final boolean configureDefault,
                                         final Map<String, NutDao> daoMap,
                                         final Map<String, NutsHeap> heapMap,
                                         final List<ObjectBuilderFactory<Engine>.KnownType> knownTypes)
            throws IOException, WorkflowTemplateNotFoundException {
        final Map<String, Workflow> workflowMap = new HashMap<String, Workflow>();

        // Add all specified workflow
        for (final ContextSetting setting : taggedSettings.values()) {
            for (final Map.Entry<String, ContextBuilder.WorkflowRegistration> entry : setting.getWorkflowMap().entrySet()) {
                workflowMap.putAll(entry.getValue().getWorkflowMap(entry.getKey(), daoMap, heapMap, setting));
            }
        }

        // Create a default workflow for heaps not referenced by any workflow
        heapLoop :
        for (final NutsHeap heap : heapMap.values()) {
            for (final Workflow workflow : workflowMap.values()) {
                if (workflow.getHeap().containsHeap(heap)) {
                    continue heapLoop;
                }
            }

            // No workflow has been found: create a default with the heap ID as ID
            workflowMap.put(heap.getId(),
                    new Workflow(createHead(knownTypes, Boolean.TRUE, null), createChains(configureDefault, knownTypes, Boolean.TRUE, null), heap));
        }

        return workflowMap;
    }

    /**
     * <p>
     * Creates the engine that will be the head of the chain of responsibility.
     * </p>
     *
     * <p>
     * If an {@link com.github.wuic.engine.HeadEngine} is configured with {@link com.github.wuic.engine.EngineService#isCoreEngine()} = false,
     * it will be returned in place of any {@link com.github.wuic.engine.HeadEngine} configured with {@link com.github.wuic.engine.EngineService#isCoreEngine()} = true.
     * because extensions override core in this case.
     * </p>
     *
     * @param knownTypes a list of known types to instantiate
     * @param includeDefaultEngines if include default engines or not
     * @param ebIdsExclusions the engines to exclude
     * @return the {@link com.github.wuic.engine.HeadEngine}
     */
    @SuppressWarnings("unchecked")
    HeadEngine createHead(final List<ObjectBuilderFactory<Engine>.KnownType> knownTypes,
                          final Boolean includeDefaultEngines,
                          final String[] ebIdsExclusions) {
        if (includeDefaultEngines) {
            HeadEngine core = null;

            for (final ObjectBuilderFactory.KnownType knownType : knownTypes) {
                final EngineService annotation = EngineService.class.cast(knownType.getClassType().getAnnotation(EngineService.class));
                if (HeadEngine.class.isAssignableFrom(knownType.getClassType())
                        && annotation.injectDefaultToWorkflow()
                        && ((ebIdsExclusions == null || CollectionUtils.indexOf(knownType.getTypeName(), ebIdsExclusions) != -1))) {
                    final String id = ContextBuilder.BUILDER_ID_PREFIX + knownType.getTypeName();
                    HeadEngine engine = HeadEngine.class.cast(newEngine(id));

                    if (annotation.isCoreEngine()) {
                        core = engine;
                    } else {
                        // Extension found, use it
                        return engine;
                    }
                }
            }

            // Use core if no extension set
            return core;
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Gets the {@link Engine} produced by the builder associated to the given ID.
     * </p>
     *
     * @param engineBuilderId the builder ID
     * @return the {@link Engine}, {@code null} if nothing is associated to the ID
     */
    Engine newEngine(final String engineBuilderId) {
        for (final ContextSetting setting : taggedSettings.values()) {
            if (setting.getEngineMap().containsKey(engineBuilderId)) {
                return setting.getEngineMap().get(engineBuilderId).build();
            }
        }

        return null;
    }

    /**
     * <p>
     * Creates a new set of chains. If we don't include default engines, then the returned map will be empty.
     * </p>
     *
     * @param configureDefault configure default engines or not
     * @param knownTypes known types to create engines
     * @param includeDefaultEngines include default or not
     * @param ebIdsExclusions the default engines to exclude
     * @return the different chains
     */
    @SuppressWarnings("unchecked")
    Map<NutType, NodeEngine> createChains(final boolean configureDefault,
                                          final List<ObjectBuilderFactory<Engine>.KnownType> knownTypes,
                                          final Boolean includeDefaultEngines,
                                          final String[] ebIdsExclusions) {
        final Map<NutType, NodeEngine> chains = new HashMap<NutType, NodeEngine>();

        // Include default engines
        if (includeDefaultEngines) {
            if (!configureDefault) {
                log.warn("This builder can't include default engines to chains if you've not call configureDefault before");
                return chains;
            }

            for (final ObjectBuilderFactory.KnownType knownType : knownTypes) {
                if ((NodeEngine.class.isAssignableFrom(knownType.getClassType()))
                        && EngineService.class.cast(knownType.getClassType().getAnnotation(EngineService.class)).injectDefaultToWorkflow()
                        && ((ebIdsExclusions == null || CollectionUtils.indexOf(knownType.getTypeName(), ebIdsExclusions) != -1))) {
                    final String id = ContextBuilder.BUILDER_ID_PREFIX + knownType.getTypeName();
                    NodeEngine engine = NodeEngine.class.cast(newEngine(id));

                    // TODO: would be easier if nut types are provided by service annotation
                    for (final NutType nutType : engine.getNutTypes()) {
                        NodeEngine chain = chains.get(nutType);

                        if (chain == null) {
                            chains.put(nutType, engine);
                        } else {
                            chains.put(nutType, NodeEngine.chain(chain, engine));
                        }

                        engine = NodeEngine.class.cast(newEngine(id));
                    }
                }
            }
        }

        return chains;
    }

    /**
     * <p>
     * Applies the given property object to all registered components.
     * </p>
     *
     * @param properties the property object
     * @see TaggedSettings#applyProperty(String, String)
     */
    void applyProperties(final Properties properties) {
        for (final Object key : properties.keySet()) {
            final String property = key.toString();
            applyProperty(property, properties.getProperty(property));
        }
    }

    /**
     * <p>
     * Sets the process context for all the settings owning an existing process context that is an instance of the same
     * given object class. If given object is {@code null} it will be ignored.
     * </p>
     *
     * @param processContext the {@link com.github.wuic.ProcessContext}
     */
    public void setProcessContext(final ProcessContext processContext) {
        if (processContext != null) {
            for (final ContextSetting setting : taggedSettings.values()) {
                if (setting.getProcessContext() != null && setting.getProcessContext().getClass().equals(processContext.getClass())) {
                    setting.setProcessContext(processContext);
                }
            }
        }
    }

    /**
     * <p>
     * Applies the given property to the registered {@link NutFilter}, {@link Engine} and {@link NutDao}.
     * </p>
     *
     * @param property the property
     * @param value the property value
     * @see TaggedSettings#applyProperty(java.util.Map, String, String)
     */
    private void applyProperty(final String property, final String value) {
        if (property.startsWith(ApplicationConfig.ENGINE_PREFIX)) {
            for (final ContextSetting ctx : taggedSettings.values()) {
                applyProperty(ctx.getEngineMap(), property, value);
            }
        } else if (property.startsWith(ApplicationConfig.DAO_PREFIX)) {
            for (final ContextSetting ctx : taggedSettings.values()) {
                applyProperty(ctx.getNutDaoMap(), property, value);
            }
        } else if (property.startsWith(ApplicationConfig.FILTER_PREFIX)) {
            for (final ContextSetting ctx : taggedSettings.values()) {
                applyProperty(ctx.getNutFilterMap(), property, value);
            }
        } else {
            for (final ContextSetting ctx : taggedSettings.values()) {
                applyProperty(ctx.getEngineMap(), property, value);
                applyProperty(ctx.getNutDaoMap(), property, value);
                applyProperty(ctx.getNutFilterMap(), property, value);
            }
        }
    }

    /**
     * <p>
     * Applies the given key/value property to all components read from the builder map specified in parameter. If the
     * map is null, nothing is done. If the property is not supported by one component, then it is just ignored.
     * </p>
     *
     * @param builders the components
     * @param property the property to set
     * @param value the value associated to the property
     */
    private void applyProperty(final Map builders, final String property, final String value) {
        if (builders != null) {
            for (final Object component : builders.values()) {
                try {
                    ObjectBuilder.class.cast(component).property(property, value);
                } catch (IllegalArgumentException iae) {
                    log.trace("The property has not been set", iae);
                }
            }
        }
    }
}
