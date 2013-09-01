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


package com.github.wuic.util;

import com.github.wuic.exception.UnableToInstantiateException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This factory discovers automatically both embedded and extension sets of {@link com.github.wuic.util.GenericBuilder}
 * implementations by checking if they are in classpath or not.
 * </p>
 *
 * <p>
 * Once the factory is instantiated, it is able to create a {@link com.github.wuic.util.GenericBuilder} thanks to the simple name
 * (class name without package) of the implementation. Two classes with the same name but in two different packages can't
 * be managed.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.0
 */
public abstract class AbstractBuilderFactory<T extends GenericBuilder> {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Simple class name associated to its default constructor.
     */
    private Map<String, Constructor<?>> builders;

    /**
     * <p>
     * Private constructor that looks up for implementations classes.
     * </p>
     *
     * <p>
     * A {@link BadArgumentException} will be thrown if the class does not provide a default constructor.
     * </p>
     */
    protected AbstractBuilderFactory() {
        builders = new HashMap<String, Constructor<?>>();

        for (final String className : classes()) {
            try {
                registerBuilderClass(className);
            } catch (ClassNotFoundException cNfe) {
                // Ok, the extension has not been added to classpath so we can accept this
                log.debug("{} not available in the classpath", cNfe);
            } catch (NoSuchMethodException nSme) {
                final String message = String.format("The %s class must provide a default constructor", className);
                throw new BadArgumentException(new IllegalArgumentException(message, nSme));
            }
        }
    }

    /**
     * <p>
     * Creates a new {@link com.github.wuic.nut.NutDaoBuilder} thanks to the given builder's simple name.
     * </p>
     *
     * <p>
     * A {@link BadArgumentException} will be thrown if not builder is registered for the given name of if the
     * class can't be instantiated.
     * </p>
     *
     * @param builderName the builder's simple name
     * @return the new instance
     * @throws UnableToInstantiateException if the builder could not be instantiated
     */
    @SuppressWarnings("unchecked")
    public T create(final String builderName) throws UnableToInstantiateException {
        final Constructor<?> builder = builders.get(builderName);

        if (builder == null) {
            final String message = String.format("%s is not identified as a builder by this factory", builderName);
            throw new BadArgumentException(new IllegalArgumentException(message));
        }

        try {
            log.debug("Instantiating with {} constructor", builder.getName());
            return (T) builder.newInstance();
        } catch (Exception ex) {
            throw new UnableToInstantiateException(new IllegalArgumentException(String.format("%s can't be created", builderName), ex));
        }
    }

    /**
     * <p>
     * Tries to add a new builder class.
     * </p>
     *
     * @param className the class name
     */
    public void addBuilderClass(final String className) {
        try {
            registerBuilderClass(className);
        } catch (ClassNotFoundException cNfe) {
            final String message = String.format("The %s class is not loaded in the classpath", className);
            throw new BadArgumentException(new IllegalArgumentException(message, cNfe));
        } catch (NoSuchMethodException nSme) {
            final String message = String.format("The %s class must provide a default constructor", className);
            throw new BadArgumentException(new IllegalArgumentException(message, nSme));
        }
    }

    /**
     * <p>
     * Tries to register the given class name as a usable builder.
     * </p>
     *
     * @param className the class name
     * @throws ClassNotFoundException if class is not in classpath
     * @throws NoSuchMethodException if there is not default builder
     */
    private void registerBuilderClass(final String className) throws ClassNotFoundException, NoSuchMethodException {
        final Class<?> clazz = Class.forName(className);
        builders.put(clazz.getSimpleName(), clazz.getConstructor());
        log.info("{} registered in the factory", className);
    }

    /**
     * <p>
     * Gets all the supported types.
     * </p>
     *
     * @return the types
     */
    protected Collection<String> knownTypes() {
        return builders.keySet();
    }

    /**
     * <p>
     * Gets all the classes to be searched in the classpath.
     * </p>
     *
     * @return the possible classes
     */
    protected abstract String[] classes();
}
