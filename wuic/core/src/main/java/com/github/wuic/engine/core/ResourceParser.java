/*
 * "Copyright (c) 2015   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.engine.core;

import com.github.wuic.NutType;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.dao.core.ProxyNutDao;
import com.github.wuic.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;

/**
 * <p>
 * This abstract class is a base for script references.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
public abstract class ResourceParser {

    /**
     * The logger.
     */
    final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The captured attributes.
     */
    final Map<String, String> attributes;

    /**
     * The captured URL.
     */
    private final String url;

    /**
     * The captured content.
     */
    private char[] content;

    /**
     * The entire statement.
     */
    private final Statement statement;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param attributes the attributes
     * @param url the url
     * @param content the content
     * @param stmt the statement declaring the resource
     */
    protected ResourceParser(final Map<String, String> attributes,
                             final String url,
                             final char[] content,
                             final Statement stmt) {
        this.attributes = attributes;
        this.url = url;
        this.statement = stmt;

        if (content != null) {
            this.content = new char[content.length];
            System.arraycopy(content, 0, this.content, 0, this.content.length);
        }
    }

    /**
     * <p>
     * Indicates if the parser should tries to read ant inline content.
     * </p>

     * @return {@code true} if inline content needs to be read, {@code false} otherwise
     */
    protected abstract Boolean readInlineIfTokenNotFound();

    /**
     * <p>
     * Returns the {@code URL}.
     * </p>
     *
     * @return the URL (could be {@code null})
     */
    protected String getUrl() {
        return url;
    }

    /**
     * <p>
     * Returns the {@link com.github.wuic.NutType} of created nut only if {@link #hasUrl()} returns {@code tue}.
     * Otherwise an {@link UnsupportedOperationException} is thrown.
     * </p>
     *
     * @return the nut type
     */
    public NutType getNutType() {
        if (!hasUrl()) {
            throw new UnsupportedOperationException("hasUrl() returns false, override this method in subclass.");
        }

        try {
            return NutType.getNutType(getUrlPath());
        } catch (IllegalArgumentException iae) {
            logger.debug("Unable to parse NutType.", iae);
            return null;
        }
    }

    /**
     * <p>
     * Applies the parser to the wrapped resource and return the captured statement.
     * </p>
     *
     * @param proxy the proxy
     * @param nutName the nut name
     * @return the captured statement, {@code null} if should be ignored
     */
    public String apply(final ProxyNutDao proxy, final String nutName) {
        if (attributes.containsKey("data-wuic-skip") || getNutType() == null) {
            if (logger.isInfoEnabled()) {
                logger.info("Ignoring parsing for statement {}", statement);
            }

            return null;
        }

        if (hasUrl()) {
            return validateUrl();
        } else if (readInlineIfTokenNotFound()) {
            final String path = getPath(nutName);
            proxy.addRule(path, newNut(path));

            return statement.toString();
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Validates the internal {@code URL} and returns the statement of the asset that refers to it if validation succeeds.
     * Otherwise {@code null} is returned. An {@code URL} is valid if it's not absolute.
     * </p>
     *
     * @return the statement, {@code null} if URL is not valid
     */
    private String validateUrl() {
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//")) {
            return null;
        } else {
            return statement.toString();
        }
    }

    /**
     * <p>
     * Creates a nut representing the captured asset.
     * </p>
     *
     * @param path the path corresponding to the resource
     * @return the nut
     */
    private Nut newNut(final String path) {
        // Converts to byte
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintWriter pw = null;

        try {
            pw = new PrintWriter(bos);
            pw.write(content);
            pw.flush();
        } finally {
            IOUtils.close(pw);
        }

        // Sign content
        final MessageDigest md = IOUtils.newMessageDigest();
        md.update(bos.toByteArray());

        // Create nut
        return new ByteArrayNut(bos.toByteArray(), path, getNutType(), ByteBuffer.wrap(md.digest()).getLong(), false);
    }

    /**
     * <p>
     * Gets the statement.
     * </p>
     *
     * @return the {@link #statement}
     */
    public Statement getStmt() {
        return statement;
    }

    /**
     * <p>
     * Indicates if the parser has captured a {@code URL} or not.
     * </p>
     *
     * @return {@code true} if URL exists, {@code false} otherwise
     */
    public boolean hasUrl() {
        return url != null && !url.isEmpty();
    }

    /**
     * <p>
     * Gets the path captured by this parser. If the {@code URL} is defined, then this method returns it.
     * Otherwise it uses the given owner nut name and suffix it with a valid extension for parser's {@link NutType}.
     * </p>
     *
     * @param nutName the owner nut name
     * @return the path
     */
    public String getPath(final String nutName) {
        // URL
        if (hasUrl()) {
            return getUrlPath();
        } else {
            return String.format("%s%s", nutName, getNutType().getExtensions()[0]);
        }
    }

    /**
     * <p>
     * Returns a subsequent characters chain from {@link #url} by removing extra part after any '#' or '?' character.
     * </p>
     *
     * @return the path from the URL
     */
    private String getUrlPath() {
        // Looking for a delimiter adding extra characters after the extracted name
        int delimiter = url.indexOf('#');

        if (delimiter == -1) {
            delimiter = url.indexOf('?');
        }

        return (delimiter == -1 ? url : url.substring(0, delimiter)).trim();
    }
}

/**
 * <p>
 * This class parses links to scripts (like JS) and inline JS scripts.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
class ScriptParser extends ResourceParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean readInlineIfTokenNotFound() {
        return Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutType getNutType() {
        return hasUrl() ? super.getNutType() : NutType.JAVASCRIPT;
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param attributes the attributes
     * @param content the content
     * @param statement the statement declaring the resource
     */
    ScriptParser(final Map<String, String> attributes, final char[] content, final Statement statement) {
        super(attributes, null, content, statement);
    }

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param attributes the attributes
     * @param url the resource URL
     * @param statement the statement declaring the resource
     */
    ScriptParser(final Map<String, String> attributes, final String url, final Statement statement) {
        super(attributes, url, null, statement);
    }
}

/**
 * <p>
 * This class parses links to CSS.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
class HrefParser extends ResourceParser {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param attributes the attributes
     * @param url the resource URL
     * @param statement the statement declaring the resource
     */
    HrefParser(final Map<String, String> attributes, final String url, final Statement statement) {
        super(attributes, url, null, statement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean readInlineIfTokenNotFound() {
        return Boolean.FALSE;
    }
}

/**
 * <p>
 * This class parses 'img' tags.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
class ImgParser extends ResourceParser {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param attributes the attributes
     * @param url the resource URL
     * @param statement the statement declaring the resource
     */
    ImgParser(final Map<String, String> attributes, final String url, final Statement statement) {
        super(attributes, url, null, statement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean readInlineIfTokenNotFound() {
        return Boolean.FALSE;
    }
}

/**
 * <p>
 * This class parses '<wuic:html-import/>' tags.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.0
 */
class HtmlParser extends ResourceParser {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param attributes the attributes
     * @param url the resource URL
     * @param statement the statement declaring the resource
     */
    HtmlParser(final Map<String, String> attributes, final String url, final Statement statement) {
        super(attributes, url, null, statement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean readInlineIfTokenNotFound() {
        return Boolean.FALSE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutType getNutType() {
        return NutType.HTML;
    }
}

/**
 * <p>
 * This class parses inline CSS.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
class CssParser extends ResourceParser {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param attributes the attributes
     * @param content the content
     * @param statement the statement declaring the resource
     */
    CssParser(final Map<String, String> attributes, final char[] content, final Statement statement) {
        super(attributes, null, content, statement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean readInlineIfTokenNotFound() {
        return Boolean.TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutType getNutType() {
        return NutType.CSS;
    }
}

/**
 * <p>
 * This class always tells the caller to not replace the parsed {@code String}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.4.4
 */
class DefaultParser extends ResourceParser {

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param statement the statement declaring the resource
     */
    DefaultParser(final Statement statement) {
        super(Collections.<String, String>emptyMap(), null, null, statement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutType getNutType() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Boolean readInlineIfTokenNotFound() {
        return Boolean.FALSE;
    }
}
