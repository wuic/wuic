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

import com.github.wuic.config.bean.PropertyBean;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * An adapter for reading {@link com.github.wuic.config.bean.BuilderBean#properties} from JSON.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class PropertyAdapter extends TypeAdapter {

    /**
     * Unmarshaller.
     */
    private final Gson gson;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param gson the unmarshaller
     */
    public PropertyAdapter(final Gson gson) {
        this.gson = gson;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final JsonWriter out, final Object value) throws IOException {
        gson.toJson(value, value.getClass(), out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object read(final JsonReader in) throws IOException {
        final List<PropertyBean> retval = new ArrayList<PropertyBean>();
        in.beginObject();

        while (in.hasNext()) {
            retval.add(new PropertyBean(in.nextName(), in.nextString()));
        }

        in.endObject();

        return retval;
    }
}
