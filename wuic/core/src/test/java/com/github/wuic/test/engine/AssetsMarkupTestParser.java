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


package com.github.wuic.test.engine;

import com.github.wuic.engine.core.AssetsMarkupHandler;
import com.github.wuic.engine.core.AssetsMarkupParser;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * {@link AssetsMarkupParser} for unit tests in {@link HtmlInspectorEngineTest}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class AssetsMarkupTestParser implements AssetsMarkupParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public void parse(final Reader reader, final AssetsMarkupHandler handler) {
        final char[] buff = new char[6];
        Map<String, String> attr = new LinkedHashMap<String, String>();

        try {
            reader.read(buff);
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }

        // break.html
        if ("<html>".equals(new String(buff))) {
            attr.put("type", "text/javascript");
            attr.put("data-wuic-break", null);
            handler.handleScriptLink("script/script1.js", attr, 3, 9, 3, 66);
            handler.handleScriptLink("script/script2.js", attr, 4, 9, 4, 82);
            attr.remove("data-wuic-break");
            handler.handleScriptLink("script/script3.js", attr, 5, 9, 5, 66);
            attr.put("data-wuic-break", null);
            handler.handleScriptLink("script/script4.js", attr, 6, 9, 6, 82);
            return;
        }

        handler.handleLink("http://bar.com/script.css", new HashMap<String, String>(), 3, 9, 3, 49);

        attr.put("type", "text/javascript");
        handler.handleScriptLink("http://foo.com/script.js", attr, 4, 9, 4, 72);

        handler.handleLink("favicon.ico", new HashMap<String, String>(), 5, 9, 5, 36);
        handler.handleLink("script/script1.css", new HashMap<String, String>(), 6, 9, 6, 44);
        handler.handleComment("<!-- some comments -->".toCharArray(), 7, 9, 24);

        attr = new LinkedHashMap<String, String>();
        attr.put("rel", "text/css");
        handler.handleLink("script/script2.css", new HashMap<String, String>(), 8, 9, 8, 59);

        attr = new LinkedHashMap<String, String>();
        attr.put("type", "text/javascript");
        handler.handleScriptLink("script/script1.js", attr, 10, 9, 10, 68);

        handler.handleScriptLink("script/foo.ts", new HashMap<String, String>(), 12, 9, 12, 46);
        handler.handleImport("heap", new HashMap<String, String>(), 13, 9, 13, 46);
        handler.handleCssContent("\n            .inner {\n            }\n        ".toCharArray(),
                new HashMap<String, String>(), 14, 9, 16, 17);

        handler.handleImgLink("earth.jpg", new HashMap<String, String>(), 21, 9, 21, 31);

        attr = new LinkedHashMap<String, String>();
        attr.put("height", "60%");
        attr.put("width", "50%");
        handler.handleImgLink("template-img.png", attr, 22, 9, 22, 70);

        attr = new LinkedHashMap<String, String>();
        attr.put("rel", "stylesheet");
        handler.handleLink("script/script3.css?foo", new HashMap<String, String>(), 24, 5, 24, 59);

        handler.handleScriptLink("script/script2.js#bar", new HashMap<String, String>(), 25, 5, 25, 43);
        handler.handleLink("script/script4.css", new HashMap<String, String>(), 26, 5, 26, 35);
        handler.handleJavascriptContent("console.log(i);".toCharArray(), new HashMap<String, String>(), 27, 5, 27, 37);

        attr = new LinkedHashMap<String, String>();
        attr.put("type", "text/javascript");
        handler.handleScriptLink("script/script3.js", attr, 28, 5, 28, 69);

        handler.handleScriptLink("script/script4.js", new HashMap<String, String>(), 29, 5, 29, 37);
    }
}
