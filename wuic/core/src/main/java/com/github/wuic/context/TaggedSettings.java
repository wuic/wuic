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


package com.github.wuic.context;

import com.github.wuic.NutType;
import com.github.wuic.ProcessContext;
import com.github.wuic.Profile;
import com.github.wuic.Workflow;
import com.github.wuic.WorkflowTemplate;
import com.github.wuic.config.ObjectBuilder;
import com.github.wuic.config.ObjectBuilderFactory;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineService;
import com.github.wuic.engine.HeadEngine;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.DuplicatedRegistrationException;
import com.github.wuic.exception.WorkflowTemplateNotFoundException;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.nut.dao.NutDao;
import com.github.wuic.nut.filter.NutFilter;
import com.github.wuic.util.CollectionUtils;
import com.github.wuic.util.Consumer;
import com.github.wuic.util.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>
 * This class wraps a map associating arbitrary tag objects to their {@link ContextSetting}. On top of this map, a lot
 * of utility methods are provided to help the {@link ContextBuilder} when building the {@link Context}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.1
 */
public class TaggedSettings extends ContextInterceptorAdapter {

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
     * @param profiles the profiles to be accepted
     * @param accept {@code true} if profiles must just be accepted by the setting, {@code false} if thet must exactly match
     * @return the registration, {@code null} if no registration has been found
     */
    ContextBuilder.NutDaoRegistration getNutDaoRegistration(final String id, final Collection<String> profiles, boolean accept)
            throws DuplicatedRegistrationException {
        final List<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration>> list =
                new ArrayList<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration>>();

        consumeSettings(new Consumer<ContextSetting>() {
            @Override
            public void apply(final ContextSetting consumed) {
                for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration> entry : consumed.getNutDaoMap().entrySet()) {
                    if (entry.getKey().getId().equals(id) && acceptProfiles(entry.getValue().getNutDaoBuilder(), profiles)) {
                        list.add(entry);
                    }
                }
            }
        }, profiles, accept);

        final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration> res = getOne(list, profiles);
        return res == null ? null : res.getValue();
    }

    /**
     * <p>
     * Removes the {@link NutDao} registration associated to the given ID. The removed
     * {@link ContextBuilder.NutDaoRegistration} is {@link ContextBuilder.NutDaoRegistration#free()}.
     * </p>
     *
     * @param id the ID
     */
    void removeNutDaoRegistration(final ContextBuilder.RegistrationId id) {
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
    void removeWorkflowRegistration(final ContextBuilder.RegistrationId id) {
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
    void removeHeapRegistration(final ContextBuilder.RegistrationId id) {
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
    void removeNutFilter(final ContextBuilder.RegistrationId id) {
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
    void removeEngine(final ContextBuilder.RegistrationId id) {
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
            contextBuilder.getSetting().getRequiredProfiles().addAll(s.getRequiredProfiles());
            contextBuilder.processContext(s.getProcessContext());

            for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration> entry : s.getNutDaoMap().entrySet()) {
                contextBuilder.nutDao(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<Engine>> entry : s.getEngineMap().entrySet()) {
                contextBuilder.engineBuilder(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<NutFilter>> entry : s.getNutFilterMap().entrySet()) {
                contextBuilder.nutFilter(entry.getKey(), entry.getValue());
            }

            for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.HeapRegistration> entry : s.getNutsHeaps().entrySet()) {
                s.getNutsHeaps().remove(entry.getKey());
                final ContextSetting setting = contextBuilder.getSetting();
                setting.getNutsHeaps().put(entry.getKey(), entry.getValue());
                taggedSettings.put(currentTag, setting);
            }

            for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.WorkflowTemplateRegistration> entry : s.getTemplateMap().entrySet()) {
                s.getTemplateMap().remove(entry.getKey());
                final ContextSetting setting = contextBuilder.getSetting();
                setting.getTemplateMap().put(entry.getKey(), entry.getValue());
                taggedSettings.put(currentTag, setting);
            }

            for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.WorkflowRegistration> entry : s.getWorkflowMap().entrySet()) {
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
     * Notifies the given consumer with any {@link ContextSetting} accepting the given profiles.
     * </p>
     *
     * @param consumer the consumer
     * @param activeProfiles the profiles
     */
    void consumeSettings(final Consumer<ContextSetting> consumer, final Collection<String> activeProfiles) {
        consumeSettings(consumer, activeProfiles, true);
    }

    /**
     * <p>
     * Notifies the given consumer with any {@link ContextSetting} accepting or exactly matching the given profiles
     * according to the flag specified in parameter.
     * </p>
     *
     * @param consumer the consumer
     * @param accept {@code true} if profiles must just be accepted by the setting, {@code false} if they must exactly match
     * @param activeProfiles the profiles
     */
    void consumeSettings(final Consumer<ContextSetting> consumer, final Collection<String> activeProfiles, final boolean accept) {
        for (final ContextSetting setting : taggedSettings.values()) {
            if ((accept && setting.acceptProfiles(activeProfiles))
                    || (!accept && setting.getRequiredProfiles().equals(new HashSet<String>(activeProfiles)))) {
                consumer.apply(setting);
            }
        }
    }

    /**
     * <p>
     * Indicates if the profiles contained in any {@link Profile} annotation of object type produced by the given builder
     * are accepted againts the profiles collection specified in parameter.
     * </p>
     *
     * @param objectBuilder the object builder
     * @return {@code true} if profiles in annotation are accepted or if there is no annotation, {@code false} otherwise
     */
    boolean acceptProfiles(final ObjectBuilder<?> objectBuilder, final Collection<String> profiles) {
        final Class<?> clazz = objectBuilder.getType();
        return !clazz.isAnnotationPresent(Profile.class)
                || ContextSetting.acceptProfiles(Arrays.asList(clazz.getAnnotation(Profile.class).value()), profiles);
    }

    /**
     * <p>
     * Gets the {@link NutFilter filters} associated to their ID currently configured in all settings.
     * </p>
     *
     * @param profiles the active profiles
     * @return the filters
     */
    Map<String, NutFilter> getFilterMap(final Collection<String> profiles) {
        final Map<String, NutFilter> retval = new HashMap<String, NutFilter>();
        final Map<String, List<Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<NutFilter>>>> registrationMap =
                new HashMap<String, List<Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<NutFilter>>>>();

        // Organize all registrations grouped by associated keys in order to detect duplicates
        consumeSettings(new Consumer<ContextSetting>() {
            @Override
            public void apply(final ContextSetting consumed) {

                // Filter the object builder's profile
                for (final Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<NutFilter>> filter : consumed.getNutFilterMap().entrySet()) {
                    if (!acceptProfiles(filter.getValue(), profiles)) {
                        continue;
                    }

                    List<Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<NutFilter>>> keys =
                            registrationMap.get(filter.getKey().getId());

                    if (keys == null) {
                        keys = new ArrayList<Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<NutFilter>>>();
                        registrationMap.put(filter.getKey().getId(), keys);
                    }

                    keys.add(filter);
                }
            }
        }, profiles);

        // Build each filter, checking for duplicated registration
        for (final Map.Entry<String, List<Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<NutFilter>>>> registration : registrationMap.entrySet()) {
            final Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<NutFilter>> filter = checkDuplicates(registration.getValue(), profiles);
            retval.put(filter.getKey().getId(), filter.getValue().build());
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
     * @param profiles the active profiles
     * @return the matching {@link com.github.wuic.context.ContextBuilder.HeapRegistration registration}
     */
    Map<String, ContextBuilder.HeapRegistration> getNutsHeap(final String regex, final Collection<String> profiles) {
        final Map<String, ContextBuilder.HeapRegistration> retval = new HashMap<String, ContextBuilder.HeapRegistration>();
        final Pattern pattern = Pattern.compile(regex);

        consumeSettings(new Consumer<ContextSetting>() {
            @Override
            public void apply(final ContextSetting setting) {
                for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.HeapRegistration> entry : setting.getNutsHeaps().entrySet()) {
                    if (pattern.matcher(entry.getKey().getId()).matches()) {
                        retval.put(entry.getKey().getId(), entry.getValue());
                    }
                }
            }
        }, profiles);

        return retval;
    }

    /**
     * <p>
     * Gets the {@link WorkflowTemplate} associated to the given ID.
     * </p>
     *
     * @param id the ID
     * @param profiles the profiles to accept
     * @return the matching {@link WorkflowTemplate template}
     * @throws WorkflowTemplateNotFoundException if no template has been found
     * @throws com.github.wuic.exception.DuplicatedRegistrationException if duplicated registrations have been found
     */
    WorkflowTemplate getWorkflowTemplate(final String id, final Collection<String> profiles)
            throws WorkflowTemplateNotFoundException, DuplicatedRegistrationException {
        final List<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.WorkflowTemplateRegistration>> collected =
                new ArrayList<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.WorkflowTemplateRegistration>>();

        // Check if an active setting contains the requested profile
        consumeSettings(new Consumer<ContextSetting>() {
            @Override
            public void apply(final ContextSetting setting) {
                for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.WorkflowTemplateRegistration> entry : setting.getTemplateMap().entrySet()) {
                    if (entry.getKey().getId().equals(id)) {
                        collected.add(entry);
                    }
                }
            }
        }, profiles);

        final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.WorkflowTemplateRegistration> res = getOne(collected, profiles);

        if (res == null) {
            WuicException.throwWorkflowTemplateNotFoundException(id);
            return null;
        } else {
            return res.getValue().getTemplate(profiles);
        }
    }

    /**
     * <p>
     * Gets one result from the given list of entries. If the list is empty, {@code null} is returned.
     * If more than one element is contained, an {@link com.github.wuic.exception.DuplicatedRegistrationException} is thrown.
     * </p>
     *
     * @param list the list
     * @param <V> the entry value
     * @return the single list value, {@code null} if the list is empty
     * @throws com.github.wuic.exception.DuplicatedRegistrationException if the list contains more than one item
     */
    private <V> Map.Entry<ContextBuilder.RegistrationId, V> getOne(final List<Map.Entry<ContextBuilder.RegistrationId, V>> list,
                                                                   final Collection<String> activeProfiles)
            throws DuplicatedRegistrationException {

        // No result found
        if (list.isEmpty()) {
            return null;
        } else {
            // Get the single result
            return checkDuplicates(list, activeProfiles);
        }
    }

    /**
     * <p>
     * Checks that the given list does not contain more that one element and returns the single element from it.
     * A {@link DuplicatedRegistrationException} will be thrown if more than one element is present.
     * </p>
     *
     * @param list the list
     * @param activeProfiles the profiles currently active
     * @param <V> the type of registration
     * @return the single element of the list
     */
    private <V> Map.Entry<ContextBuilder.RegistrationId, V> checkDuplicates(final List<Map.Entry<ContextBuilder.RegistrationId, V>> list,
                                                                            final Collection<String> activeProfiles) {
        if (list.size() > 1) {
            // Multiple registration found, throw proper exception message
            final List<Object> ids = new ArrayList<Object>(list.size());

            for (final Map.Entry<ContextBuilder.RegistrationId, V> o : list) {
                ids.add(o.getKey());
            }

            WuicException.throwDuplicateRegistrationException(ids, activeProfiles);
        }

        return list.get(0);
    }

    /**
     * <p>
     * Gets all the {@link NutDao} created with the existing registrations.
     * </p>
     *
     * @param profiles the active profiles
     * @return the {@link NutDao} associated to their registration ID
     */
    Map<String, NutDao> getNutDaoMap(final Collection<String> profiles) {
        final Map<String, NutDao> daoMap = new HashMap<String, NutDao>();
        final Map<String, List<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration>>> registrationMap =
                new HashMap<String, List<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration>>>();

        // Organize all registrations grouped by associated keys in order to detect duplicates
        consumeSettings(new Consumer<ContextSetting>() {
            @Override
            public void apply(final ContextSetting consumed) {

                // Filter the object builder's profile
                for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration> dao : consumed.getNutDaoMap().entrySet()) {
                    if (!acceptProfiles(dao.getValue().getNutDaoBuilder(), profiles)) {
                        continue;
                    }

                    List<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration>> keys =
                            registrationMap.get(dao.getKey().getId());

                    if (keys == null) {
                        keys = new ArrayList<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration>>();
                        registrationMap.put(dao.getKey().getId(), keys);
                    }

                    keys.add(dao);
                }
            }
        }, profiles);

        // Populate the DAO map with the same instance associated to all keys sharing the same registration
        final List<ProxyNutDaoRegistration> proxyRegistrations = new ArrayList<ProxyNutDaoRegistration>();
        for (final Map.Entry<String, List<Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration>>> registration : registrationMap.entrySet()) {
            final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.NutDaoRegistration> dao = checkDuplicates(registration.getValue(), profiles);
            daoMap.put(dao.getKey().getId(), dao.getValue().getNutDao(proxyRegistrations));
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
     * @param daoMap the map of existing DAOs
     * @param nutFilterMap the map of existing filters
     * @param profiles the active profiles
     * @return the {@link NutsHeap} associated to their registration ID
     */
    Map<String, NutsHeap> getNutsHeapMap(final Map<String, NutDao> daoMap,
                                         final Map<String, NutFilter> nutFilterMap,
                                         final Collection<String> profiles)
            throws IOException {
        final Map<String, NutsHeap> heapMap = new HashMap<String, NutsHeap>();

        // The composite heap must be read after the standard heap
        for (final ContextSetting setting : taggedSettings.values()) {
            // Required profile not enabled
            if (!setting.acceptProfiles(profiles)) {
                continue;
            }

            for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.HeapRegistration> heap : setting.getNutsHeaps().entrySet()) {
                if (!heap.getValue().isComposition()) {
                    final ContextBuilder.HeapRegistration r = heap.getValue();
                    heapMap.put(heap.getKey().getId(), r.getHeap(heap.getKey().getId(), daoMap, heapMap, nutFilterMap, setting));
                }
            }
        }

        // Now read all compositions
        for (final ContextSetting setting : taggedSettings.values()) {
            // Required profile not enabled
            if (!setting.acceptProfiles(profiles)) {
                continue;
            }

            for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.HeapRegistration> heap : setting.getNutsHeaps().entrySet()) {
                if (heap.getValue().isComposition()) {
                    final ContextBuilder.HeapRegistration r = heap.getValue();
                    heapMap.put(heap.getKey().getId(), r.getHeap(heap.getKey().getId(), daoMap, heapMap, nutFilterMap, setting));
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
     * @param configureDefault {@code true} if default configuration for engine should be injected to workflow, {@code false} otherwise
     * @param heapMap the map of known heaps
     * @param knownTypes the known engine builder type
     * @param profiles the active profiles
     * @return the {@link Workflow} associated to their registration ID
     * @throws com.github.wuic.exception.DuplicatedRegistrationException if duplicated registrations have been found
     */
    Map<String, Workflow> getWorkflowMap(final boolean configureDefault,
                                         final Map<String, NutsHeap> heapMap,
                                         final List<ObjectBuilderFactory<Engine>.KnownType> knownTypes,
                                         final Collection<String> profiles)
            throws IOException, WorkflowTemplateNotFoundException, DuplicatedRegistrationException {
        final Map<String, Workflow> workflowMap = new HashMap<String, Workflow>();

        // Add all specified workflow
        for (final ContextSetting setting : taggedSettings.values()) {
            // Required profile not enabled
            if (!setting.acceptProfiles(profiles)) {
                continue;
            }

            // Generate the set of workflow for each registration and check for duplicates
            for (final Map.Entry<ContextBuilder.RegistrationId, ContextBuilder.WorkflowRegistration> entry : setting.getWorkflowMap().entrySet()) {
                final Map<String, Workflow> res = entry.getValue().getWorkflowMap(entry.getKey().getId(), heapMap, setting, profiles);
                final Set<String> a = res.keySet();
                final Set<String> b = workflowMap.keySet();
                checkDuplicate(a, b, profiles);
                checkDuplicate(b, a, profiles);
                workflowMap.putAll(res);
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

            // Check that a workflow does not already exists for the heap ID
            if (workflowMap.containsKey(heap.getId())) {
                log.error("Duplicated registration: a workflow has the same ID as a heap ID not explicitly referenced by a workflow.");
                WuicException.throwDuplicateRegistrationException(Arrays.asList((Object) heap.getId()), profiles);
            }

            // No workflow has been found: create a default with the heap ID as ID
            final HeadEngine head = createHead(knownTypes, configureDefault, null, profiles);
            final Map<NutType, NodeEngine> chains = createChains(configureDefault, knownTypes, Boolean.TRUE, null, profiles);
            workflowMap.put(heap.getId(), new Workflow(head, chains, heap));
        }

        return workflowMap;
    }

    /**
     * <p>
     * Checks for duplicated IDs by checking if an element of {@code a} exists in {@code b} and in that case throws
     * a {@link DuplicatedRegistrationException}.
     * </p>
     *
     * @param a set a
     * @param b set b
     * @param profiles current active profiles
     */
    private void checkDuplicate(final Set<String> a, final Set<String> b, final Collection<String> profiles) {
        for (final String idA : a) {
            if (b.contains(idA)) {
                WuicException.throwDuplicateRegistrationException(Arrays.asList((Object) idA), profiles);
            }
        }
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
     * @param ebTypesExclusion the engines types to exclude
     * @param profiles the profiles
     * @return the {@link com.github.wuic.engine.HeadEngine}
     * @throws com.github.wuic.exception.DuplicatedRegistrationException if duplicated registrations have been found
     */
    @SuppressWarnings("unchecked")
    HeadEngine createHead(final List<ObjectBuilderFactory<Engine>.KnownType> knownTypes,
                          final Boolean includeDefaultEngines,
                          final String[] ebTypesExclusion,
                          final Collection<String> profiles) throws DuplicatedRegistrationException {
        if (includeDefaultEngines) {
            HeadEngine core = null;

            for (final ObjectBuilderFactory.KnownType knownType : knownTypes) {
                final EngineService annotation = EngineService.class.cast(knownType.getClassType().getAnnotation(EngineService.class));
                if (HeadEngine.class.isAssignableFrom(knownType.getClassType())
                        && annotation.injectDefaultToWorkflow()
                        && ((ebTypesExclusion == null || CollectionUtils.indexOf(knownType.getTypeName(), ebTypesExclusion) != -1))) {
                    final String id = ContextBuilder.getDefaultBuilderId(knownType.getClassType());
                    HeadEngine engine = HeadEngine.class.cast(newEngine(id, profiles));

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
     * An {@code IllegalStateException} is thrown if no engine is found.
     * </p>
     *
     * @param engineBuilderId the builder ID
     * @return the {@link Engine}
     * @throws com.github.wuic.exception.DuplicatedRegistrationException if duplicated registrations have been found
     */
    Engine newEngine(final String engineBuilderId, final Collection<String> profiles) throws DuplicatedRegistrationException {
        final List<Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<Engine>>> list =
                new ArrayList<Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<Engine>>>();

        consumeSettings(new Consumer<ContextSetting>() {
            @Override
            public void apply(final ContextSetting consumed) {
                for (final Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<Engine>> entry : consumed.getEngineMap().entrySet()) {
                    if (entry.getKey().getId().equals(engineBuilderId) && acceptProfiles(entry.getValue(), profiles)) {
                        list.add(entry);
                    }
                }
            }
        }, profiles);

        final Map.Entry<ContextBuilder.RegistrationId, ObjectBuilder<Engine>> res = getOne(list, profiles);

        if (res == null) {
            throw new IllegalStateException(String.format("'%s' not associated to any %s", engineBuilderId, EngineService.class.getName()));
        } else {
            return res.getValue().build();
        }
    }

    /**
     * <p>
     * Creates a new set of chains. If we don't include default engines, then the returned map will be empty.
     * </p>
     *
     * @param configureDefault configure default engines or not
     * @param knownTypes known types to create engines
     * @param includeDefaultEngines include default or not
     * @param ebTypesExclusion the default engines to exclude
     * @param profiles the profiles to be accepted
     * @return the different chains
     * @throws com.github.wuic.exception.DuplicatedRegistrationException if duplicated registrations have been found
     */
    @SuppressWarnings("unchecked")
    Map<NutType, NodeEngine> createChains(final boolean configureDefault,
                                          final List<ObjectBuilderFactory<Engine>.KnownType> knownTypes,
                                          final Boolean includeDefaultEngines,
                                          final String[] ebTypesExclusion,
                                          final Collection<String> profiles) throws DuplicatedRegistrationException {
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
                        && ((ebTypesExclusion == null || CollectionUtils.indexOf(knownType.getTypeName(), ebTypesExclusion) == -1))) {
                    final String id = ContextBuilder.getDefaultBuilderId(knownType.getClassType());
                    NodeEngine engine = NodeEngine.class.cast(newEngine(id, profiles));

                    // TODO: would be easier if nut types are provided by service annotation
                    for (final NutType nutType : engine.getNutTypes()) {
                        NodeEngine chain = chains.get(nutType);

                        if (chain == null) {
                            chains.put(nutType, engine);
                        } else {
                            chains.put(nutType, NodeEngine.chain(chain, engine));
                        }

                        engine = NodeEngine.class.cast(newEngine(id, profiles));
                    }
                }
            }
        }

        return chains;
    }

    /**
     * <p>
     * Applies the given {@link PropertyResolver} to the registered {@link NutFilter}, {@link Engine} and {@link NutDao}.
     * </p>
     *
     * @param propertyResolver the property resolver
     * @see TaggedSettings#applyProperties(java.util.Map, com.github.wuic.util.PropertyResolver)
     */
    void applyProperties(final PropertyResolver propertyResolver) {
        for (final ContextSetting ctx : taggedSettings.values()) {
            applyProperties(ctx.getEngineMap(),propertyResolver);
            applyProperties(ctx.getNutDaoMap(), propertyResolver);
            applyProperties(ctx.getNutFilterMap(), propertyResolver);
        }
    }

    /**
     * <p>
     * This method refreshes the given {@link ContextSetting} and its dependent {@link ContextSetting settings}.
     * A setting is dependent from any other if:
     * <ul>
     *     <li>
     *         one of its {@link ContextBuilder.HeapRegistration} is a composition of another registration from the
     *         other setting
     *     </li>
     *     <li>
     *         one of its {@link ContextBuilder.HeapRegistration} refers a {@link ContextBuilder.NutDaoRegistration}
     *         from the other setting
     *     </li>
     *     <li>
     *         one of its {@link ContextBuilder.WorkflowRegistration} refers a {@link ContextBuilder.NutDaoRegistration store}
     *         in its referenced template from the other setting
     *     </li>
     *     <li>
     *         one of its {@link ContextBuilder.WorkflowRegistration} refers a {@link ContextBuilder.HeapRegistration heap}
     *     </li>
     *     <li>
     *         one of its {@link ContextBuilder.WorkflowTemplateRegistration} refers a {@link ContextBuilder.NutDaoRegistration store}
     *         from the other setting
     *     </li>
     *
     *     <li>
     *         one of its {@link ContextBuilder.NutDaoRegistration} proxy another DAO from the other setting
     *     </li>
     * </ul>
     * </p>
     */
    void refreshDependencies(final ContextSetting setting) {
        refresh(new ArrayList<ContextSetting>(), setting);
    }

    /**
     * <p>
     * Refresh the dependencies of settings containing one the given profile.
     * </p>
     *
     * @param profiles the profiles contained in the settings to be refreshed
     * @see #refreshDependencies(ContextSetting)
     */
    void refreshDependencies(final String ... profiles) {
        for (final ContextSetting setting : taggedSettings.values()) {
            for (final String profile : profiles) {
                if (setting.getRequiredProfiles().contains(profile)) {
                    refreshDependencies(setting);
                    break;
                }
            }
        }
    }

    /**
     * <p>
     * This method detects the dependencies of the given setting and refresh them.
     * All dependency type described by {@link #refreshDependencies(ContextSetting)} are checked here.
     * </p>
     *
     * @param path all settings that have been refreshed
     * @param setting the setting to evaluate
     */
    private void refreshDependencies(final List<ContextSetting> path, final ContextSetting setting) {
        // Cycle detected
        if (path.contains(setting)) {
            return;
        }

        path.add(setting);

        // Check dependencies in each setting
        for (final ContextSetting contextSetting : taggedSettings.values()) {

            // Ignore current setting
            if (contextSetting != setting) {
                forceRefreshComposition(path, setting, contextSetting);
                forceRefreshHeapDao(path, setting, contextSetting);
                forceRefreshStore(path, setting, contextSetting);
                forceWorkflowHeap(path, setting, contextSetting);
                forceRefreshProxyDao(path, setting, contextSetting);
            }
        }
    }

    /**
     * <p>
     * Checks if the given candidate contains a heap referenced by the setting specified in parameter.
     * </p>
     *
     * @param path the already refreshed settings
     * @param setting the setting that will refresh its dependencies
     * @param candidate the potential dependency
     */
    private void forceRefreshComposition(final List<ContextSetting> path, final ContextSetting setting, final ContextSetting candidate) {

        // No heap registration to test
        if (setting.getNutsHeaps().isEmpty()) {
            return;
        }

        // Looking for a heap registration referenced by a composition of the current setting
        for (final ContextBuilder.HeapRegistration registration : candidate.getNutsHeaps().values()) {

            // Candidate has a heap registration that refers another one, we check if this other heap is refreshed
            if (registration.getHeapsIds() != null) {
                for (final ContextBuilder.RegistrationId heapId : setting.getNutsHeaps().keySet()) {

                    // Dependency found: change state of this dependency and recursively search its own dependencies
                    if (registration.getHeapsIds().contains(heapId.getId())) {
                        refresh(path, candidate);
                        return;
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Checks if the given candidate contains a DAP referenced by the heap of the setting specified in parameter.
     * </p>
     *
     * @param path the already refreshed settings
     * @param setting the setting that will refresh its dependencies
     * @param candidate the potential dependency
     */
    private void forceRefreshHeapDao(final List<ContextSetting> path, final ContextSetting setting, final ContextSetting candidate) {

        // No DAO registration to test
        if (setting.getNutDaoMap().isEmpty()) {
            return;
        }

        // Looking for a DAO registration referenced by a heap registration of the current setting
        loop:
        for (final ContextBuilder.HeapRegistration registration : candidate.getNutsHeaps().values()) {

            // Dependency found: change state of this dependency and recursively search its own dependencies
            for (final ContextBuilder.RegistrationId registrationId : setting.getNutDaoMap().keySet()) {
                if (registrationId.getId().equals(registration.getNutDaoId())) {
                    refresh(path, candidate);
                    break loop;
                }
            }
        }
    }

    /**
     * <p>
     * Checks if the given candidate contains a DAO referenced by a template of the setting specified in parameter.
     * </p>
     *
     * @param path the already refreshed settings
     * @param setting the setting that will refresh its dependencies
     * @param candidate the potential dependency
     */
    private void forceRefreshStore(final List<ContextSetting> path, final ContextSetting setting, final ContextSetting candidate) {

        // No template registration to test
        if (setting.getTemplateMap().isEmpty()) {
            return;
        }

        // Looking for a template referenced by a workflow registration of the current setting
        loop:
        for (final ContextBuilder.WorkflowRegistration registration : candidate.getWorkflowMap().values()) {

            // Dependency found: change state of this dependency and recursively search its own dependencies
            if (registration.getWorkflowTemplateId() != null) {
                for (final ContextBuilder.RegistrationId registrationId : setting.getTemplateMap().keySet()) {
                    if (registrationId.getId().equals(registration.getWorkflowTemplateId())) {
                        refresh(path, candidate);
                        break loop;
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Checks if the given candidate contains a template referencing by a heap of the setting specified in parameter.
     * </p>
     *
     * @param path the already refreshed settings
     * @param setting the setting that will refresh its dependencies
     * @param candidate the potential dependency
     */
    private void forceWorkflowHeap(final List<ContextSetting> path, final ContextSetting setting, final ContextSetting candidate) {

        // No heap registration to test
        if (candidate.getNutsHeaps().isEmpty()) {
            return;
        }

        // Looking for a heap referenced by a workflow registration of the current setting
        for (final ContextBuilder.WorkflowRegistration registration : setting.getWorkflowMap().values()) {
            final Pattern heapPattern = Pattern.compile(registration.getHeapIdPattern());

            // Each ID of registration is evaluated to check if it matches the pattern
            for (final ContextBuilder.RegistrationId heapId : candidate.getNutsHeaps().keySet()) {

                // Dependency found: change state of this dependency and recursively search its own dependencies
                if (heapPattern.matcher(heapId.getId()).matches()) {
                    refresh(path, candidate);
                    return;
                }
            }
        }
    }

    /**
     * <p>
     * Checks if the given candidate contains a DAO referenced by a proxy of the setting specified in parameter.
     * </p>
     *
     * @param path the already refreshed settings
     * @param setting the setting that will refresh its dependencies
     * @param candidate the potential dependency
     */
    private void forceRefreshProxyDao(final List<ContextSetting> path, final ContextSetting setting, final ContextSetting candidate) {

        // No DAO registration to compare
        if (candidate.getNutDaoMap().isEmpty()) {
            return;
        }

        // Looking for a DAO referenced by another DAO registration that proxy it in the current setting
        for (final ContextBuilder.RegistrationId nutDaoId : setting.getNutDaoMap().keySet()) {
            for (final ContextBuilder.NutDaoRegistration registration : candidate.getNutDaoMap().values()) {

                // Dependency found: change state of this dependency and recursively search its own dependencies
                if (registration.getProxyDao().values().contains(nutDaoId.getId())) {
                    refresh(path, candidate);
                    return;
                }
            }
        }
    }

    /**
     * <p>
     * Refresh the given setting and refresh its dependencies.
     * </p>
     *
     * @param path the already refreshed settings
     * @param setting the setting to refresh
     */
    private void refresh(final List<ContextSetting> path, final ContextSetting setting) {
        for (final ContextBuilder.NutDaoRegistration dao : setting.getNutDaoMap().values()) {
            dao.free();
        }

        // Notifies any listeners to clear any cache
        for (final ContextBuilder.HeapRegistration heap : setting.getNutsHeaps().values()) {
            heap.free();
        }

        refreshDependencies(path, setting);
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
     * Retrieve all supported properties by the given map of builders from the {@link PropertyResolver} specified in parameter
     * and apply the value if not {@code null}.
     * </p>
     *
     * @param builders the components
     * @param propertyResolver the property resolver
     */
    private void applyProperties(final Map<ContextBuilder.RegistrationId, ? extends ObjectBuilder> builders,
                                 final PropertyResolver propertyResolver) {
        if (builders != null) {
            for (final ObjectBuilder component : builders.values()) {
                component.configure(propertyResolver);
            }
        }
    }
}
