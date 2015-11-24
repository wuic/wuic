package com.github.wuic.test.engine;

import com.github.wuic.engine.core.AssetsMarkupHandler;
import com.github.wuic.engine.core.AssetsMarkupParser;

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
        handler.handleLink("http://bar.com/script.css", new HashMap<String, String>(), 3, 9, 3, 49);

        Map<String, String> attr = new LinkedHashMap<String, String>();
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
