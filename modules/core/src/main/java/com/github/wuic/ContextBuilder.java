/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic;

import com.github.wuic.factory.EngineFactoryBuilder;
import com.github.wuic.resource.WuicResourceFactoryBuilder;

import java.util.Map;

/**
 * <p>
 * This builder can be configured to build contexts in an expected state by the user.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public interface ContextBuilder {

    /**
     * <p>
     * Decorates the current builder with a new builder associated to a specified tag. Tagging the context allows to
     * isolate a set of configurations that could be erased by calling {@link ContextBuilder#clearTag(String)}.
     * This way, this feature is convenient when you need to poll the configurations to reload it.
     * </p>
     *
     * <p>
     * All configurations will be associated to the tag until the {@link com.github.wuic.ContextBuilder#releaseTag()}
     * method is called.
     * </p>
     *
     * @param tagName the tag name
     * @return the current builder which will associates all configurations to the tag
     * @see ContextBuilder#clearTag(String)
     * @see com.github.wuic.ContextBuilder#releaseTag()
     */
    ContextBuilder tag(String tagName);

    /**
     * <p>
     * Clears all configurations associated to the given tag.
     * </p>
     *
     * @param tagName the tag name
     * @return this {@link ContextBuilder}
     */
    ContextBuilder clearTag(String tagName);

    /**
     * <p>
     * Releases the current tag of this context. When the configurations associated to a tag are finished, it could be
     * released by calling this method to not tag next configurations.
     * </p>
     *
     * @return this current builder without tag
     */
    ContextBuilder releaseTag();

    /**
     * <p>
     * Add a new {@link WuicResourceFactoryBuilder} identified by the specified ID and based on a given {@link Map} of
     * properties.
     * </p>
     *
     * <p>
     * The {@link WuicResourceFactoryBuilder} class is expected to be found in the classpath, otherwise an exception
     * will be thrown when the {@link com.github.wuic.ContextBuilder#build()} method is called.
     * </p>
     *
     * @param id the ID which identifies the builder in the context
     * @param clazz the class to be instantiated
     * @param properties the properties to use to configure the builder
     * @return this {@link ContextBuilder}
     */
    ContextBuilder resourceFactoryBuilder(String id,
                                          Class<? extends WuicResourceFactoryBuilder> clazz,
                                          Map<String, String> properties);

    /**
     * <p>
     * Defines a new group in this context. A group is always identified by an ID and is associated to
     * {@link WuicResourceFactoryBuilder} to use to convert paths into {@link com.github.wuic.resource.WuicResource}.
     * A list of paths needs also to be specified to know which underlying resources compose the group.
     * </p>
     *
     * @param id the group ID
     * @param rfbId the {@link WuicResourceFactoryBuilder} the group is based on
     * @param path the path
     * @return this {@link ContextBuilder}
     */
    ContextBuilder group(String id, String rfbId, String ... path);

    /**
     * <p>
     * Declares a new {@link EngineFactoryBuilder} with its specific properties. The builder is identified by an unique
     * ID and produces in fine {@link com.github.wuic.engine.Engine engines} that could be chained.
     * </p>
     *
     * @param id the efb ID
     * @param clazz the class that implements {@link EngineFactoryBuilder}
     * @param properties the builder's properties (must be supported by the builder)
     * @return this {@link ContextBuilder}
     */
    ContextBuilder engineFactoryBuilder(String id,
                                        Class<? extends EngineFactoryBuilder> clazz,
                                        Map<String, String> properties);

    /**
     * <p>
     * Creates a new workflow. Any resources processing will be done through a existing workflow.
     * </p>
     *
     * <p>A workflow consists to chain a set of engines produced by the specified
     * {@link EngineFactoryBuilder builders} with a {@link FilesGroup group} as data to be processed. There is a chain
     * for each possible {@link FileType}. If no chain is specified for a particular {@link FileType}, then a default
     * one will be created. Moreover, default engines are injected in the chain to perform common operations to be done
     * on resources. If an {@link EngineFactoryBuilder} is specified in a chain while it is by default, then the
     * configuration of the given builder will overrides the default one.
     * </p>
     *
     * <p>
     * A set of {@link WuicResourceFactoryBuilder} could be specified to store processed resources. When the client will
     * retrieve the resources, it will access it through a proxy URI configured in the protocol. This URI corresponds to
     * a server in front of the location where resources have been stored.
     * </p>
     *
     * @param id the workflow ID
     * @param groupId the group that needs to be processed
     * @param efbId the set of {@link EngineFactoryBuilder} to use
     * @param rfbIds the set of {@link WuicResourceFactoryBuilder} where to eventually upload processed resources
     * @param includeDefaultEngines include or not default engines
     * @return this {@link ContextBuilder}
     */
    ContextBuilder workflow(String id, String groupId,
                            Map<FileType, String[]> efbId,
                            Boolean includeDefaultEngines,
                            String ... rfbIds);

    /**
     * <p>
     * Builds the context. Should throws an {@link IllegalStateException} if the context is not correctly configured.
     * For instance : associate a group to an undeclared {@link WuicResourceFactoryBuilder} ID.
     * </p>
     *
     * @return this {@link ContextBuilder}
     */
    Context build();
}
