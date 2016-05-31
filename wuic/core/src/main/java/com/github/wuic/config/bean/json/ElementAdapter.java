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


package com.github.wuic.config.bean.json;

import com.github.wuic.config.bean.HeapBean;
import com.github.wuic.config.bean.HeapReference;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This type adapter must be used to create the list affected to {@link com.github.wuic.config.bean.HeapBean#elements} attribute.
 * By reading the current path of the {@code JsonReader}, the class determines the class to instantiate for each element
 * of the JSON array. If the path contains with "nutPath", the class is {@code String}, if it's "heap", the class is
 * {@link com.github.wuic.config.bean.HeapBean} and if it's "heapId", the class is {@link com.github.wuic.config.bean.HeapReference}.
 * If contains none of the strings previously mentioned, an {@code IllegalStateException} if thrown.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class ElementAdapter<T extends Object> extends TypeAdapter<Object> {

    /**
     * The unmarshaller.
     */
    private final Gson gson;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param gson the unmarshaller
     */
    public ElementAdapter(final Gson gson) {
        this.gson = gson;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final JsonWriter jsonWriter, final Object o) throws IOException {
        gson.toJson(o, o.getClass(), jsonWriter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object read(final JsonReader jsonReader) throws IOException {
        final List<Object> objects = new ArrayList<Object>();
        final JsonToken state = jsonReader.peek();

        if (state == JsonToken.BEGIN_ARRAY) {
            jsonReader.beginArray();

            // Reading each item of the array
            while (jsonReader.hasNext()) {
                objects.add(readObject(jsonReader));
            }

            jsonReader.endArray();
        } else if (state == JsonToken.BEGIN_OBJECT || state == JsonToken.STRING) {
            objects.add(readObject(jsonReader));
        }

        return objects;
    }

    /**
     * <p>
     * Selects the right class to de-serialize according to the current path in the given {@code JsonReader} and return
     * an instance of that instance.
     * </p>
     *
     * @param jsonReader the reader
     * @return the class to use when reading current JSON object
     */
    private Object readObject(final JsonReader jsonReader) {
        final String path = jsonReader.getPath();

        if (path.contains("nutPath")) {
            return gson.fromJson(jsonReader, String.class);
        } else if (path.contains("heapId")) {
            return new HeapReference((String) gson.fromJson(jsonReader, String.class));
        } else if (path.contains("heap")) {
            return gson.fromJson(jsonReader, HeapBean.class);
        }

        throw new IllegalStateException(String.format("%s can handle only 'nutPath', 'heap' or 'heapId' names", path));
    }
}
