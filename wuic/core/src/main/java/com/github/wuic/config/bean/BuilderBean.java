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


package com.github.wuic.config.bean;

import com.github.wuic.config.bean.json.PropertyAdapterFactory;
import com.github.wuic.config.bean.json.StringArrayAdapterFactory;
import com.google.gson.annotations.JsonAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

/**
 * <p>
 * Represents a builder (engine, DAO) in bean configuration stream.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BuilderBean {

    /**
     * The builder's ID.
     */
    @XmlAttribute(name = "id")
    private String id;

    /**
     * Comma-separated list of profiles required to apply this bean.
     */
    @XmlAttribute(name = "profiles")
    @JsonAdapter(StringArrayAdapterFactory.class)
    private String profiles;

    /**
     * The builder's type.
     */
    @XmlAttribute(name = "type")
    private String type;

    /**
     * All the builder's properties.
     */
    @XmlElementWrapper(name = "properties")
    @XmlElement(name = "property")
    @JsonAdapter(PropertyAdapterFactory.class)
    private List<PropertyBean> properties;

    /**
     * <p>
     * Gets the ID.
     * </p>
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * <p>
     * Gets the profiles.
     * </p>
     *
     * @return the profiles
     */
    public String[] getProfiles() {
        return profiles != null ? profiles.split(",") : null;
    }

    /**
     * <p>
     * Gets the builder's type.
     * </p>
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * <p>
     * Gets the properties.
     * </p>
     *
     * @return the properties
     */
    public List<PropertyBean> getProperties() {
        return properties;
    }

    /**
     * <p>
     * Sets the ID.
     * </p>
     *
     * @param id the ID
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * <p>
     * Sets the type.
     * </p>
     *
     * @param type the type
     */
    public void setType(final String type) {
        this.type = type;
    }
}
