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


package com.github.wuic.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * This class holds a set of {@link PropertySetter} and manages them.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class PropertySetterRepository {

    /**
     * The property setters define which property is supported.
     * Setters are defined per method name.
     */
    private Map<String, PropertySetter[]> propertySetters;

    /**
     * Property keys.
     */
    private List<String> propertyKeys;

    /**
     * <p>
     * Creates a new instance.
     * </p>
     */
    public PropertySetterRepository() {
        propertySetters = new HashMap<String, PropertySetter[]>();
        propertyKeys = new ArrayList<String>();
    }

    /**
     * <p>
     * Returns all the property keys registered in this repository.
     * </p>
     */
    public Collection<String> getPropertyKeys() {
        return propertyKeys;
    }

    /**
     * <p>
     * Adds the given {@link PropertySetter setters}.
     * </p>
     *
     * @param methodName the method associated to setters
     * @param setters the setters
     */
    protected void addPropertySetter(final String methodName, final PropertySetter ... setters) {

        // Adds the property keys
        for (final PropertySetter<?> propertySetter : setters) {
            propertyKeys.add(propertySetter.getPropertyKey());
        }

        PropertySetter[] props = this.propertySetters.get(methodName);

        if (props == null) {
            props = new PropertySetter[setters.length];
            System.arraycopy(setters, 0, props, 0, props.length);
        } else {
            // Merges the two arrays. Each array should not be null.
            final PropertySetter[] target = new PropertySetter[setters.length + props.length];
            System.arraycopy(props, 0, target, 0, props.length);
            System.arraycopy(setters, 0, target, props.length, setters.length);
            props = target;
        }

        this.propertySetters.put(methodName, props);
    }

    /**
     * <p>
     * Returns an array with one property for each {@link PropertySetter} associated to the given method name.
     * If the property is not set, then the default value is returned.
     * </p>
     *
     * @param methodName the method name
     * @return the properties
     */
    protected Object[] getAllProperties(final String methodName) {
        final PropertySetter[] props = this.propertySetters.get(methodName);
        final Object[] retval = new Object[props.length];

        for (int i = 0; i < retval.length; i++) {
            final PropertySetter setter = props[i];
            retval[i] = setter.get();
        }

        return retval;
    }

    /**
     * <p>
     * Returns the property setter arrays.
     * </p>
     *
     * @return the property setter arrays
     */
    protected Collection<PropertySetter[]> getPropertySetters() {
        return propertySetters.values();
    }
}
