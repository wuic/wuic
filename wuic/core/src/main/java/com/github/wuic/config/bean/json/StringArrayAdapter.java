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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * <p>
 * An adapter that checks if the annotate field refers to a single string or an array of string.
 * In the former case, the string is simply read and in the later case the value value is read as an array joined with a
 * comma. The value is written as an array when the string contains comma, which means several profiles are declared.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class StringArrayAdapter extends TypeAdapter<String> {

    /**
     * Gson.
     */
    private final Gson gson;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param gson the marshaller
     */
    public StringArrayAdapter(final Gson gson) {
        this.gson = gson;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final JsonWriter out, final String value) throws IOException {
        if (value.contains(",")) {
            gson.toJson(value.split(","), String[].class, out);
        } else {
            gson.toJson(value, String.class, out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String read(final JsonReader in) throws IOException {
        if (in.peek() == JsonToken.STRING) {
            return gson.fromJson(in, String.class);
        } else {
            try {
                in.beginArray();
                final StringBuilder sb = new StringBuilder();

                // Read each value of the array
                while (in.hasNext()) {
                    sb.append(",").append(gson.fromJson(in, String.class));
                }

                return sb.length() == 0 ? "" : sb.substring(1);
            } finally {
                in.endArray();
            }
        }
    }
}
