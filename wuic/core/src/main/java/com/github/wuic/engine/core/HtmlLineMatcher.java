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

import com.github.wuic.engine.LineInspector;
import com.github.wuic.util.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 * A {@link com.github.wuic.engine.LineInspector.LineMatcher} that reads HTML content to identify assets declaration
 * like CSS, images and Javascript. It also discovers the comments and internal import statements.
 * Each time a call to {@link #find()} returns {@code true}, it means {@link #group()} will return a statement that has
 * been discovered through the stream.
 * </p>
 *
 * <p>
 * Each kind of declaration corresponds to a group number. You can call the {@link #group(int)} method by using
 * the value returned by {@code ordinal()} of any enumeration defined in {@link Group}.
 * If the {@link #group(int)} does not return {@code null}, then it means the group corresponds to the tested enumeration.
 * </p>
 *
 * <p>
 * If an existing group is a comment or a script with a content, the {@link Group#CONTENT_GROUP} group will return content
 * itself. Moreover, if a group corresponding to a tag exists, then all it attributes are available in a sequence of group
 * starting from {@code Group.values() + 1} to {@link #groupCount()}.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public class HtmlLineMatcher extends LineInspector.LineMatcher {

    public enum Group {

        JS_GROUP("script", false, false, "src"),

        CSS_REF_GROUP("link", true, false, "href"),

        IMG_GROUP("img", true, false, "src"),

        STYLE_GROUP("style", false, true, null),

        IMPORT_GROUP("html-import", true, false, "workflowId"),

        COMMENT_GROUP,

        CONTENT_GROUP;

        private String tagName;

        private boolean mustBeInline;

        private boolean cannotBeInLine;

        private String inlineAttribute;

        private Group() {
        }

        private Group(final String tag, final boolean mustInline, final boolean notInline, final String inlineAttr) {
            tagName = tag;
            mustBeInline = mustInline;
            cannotBeInLine = notInline;
            inlineAttribute = inlineAttr;
        }
    }

    private class Position {
        private int start;
        private int end;
        private String string;

        private Position(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        public int length() {
            return end - start;
        }

        @Override
        public String toString() {
            if (string == null) {
                string = group.substring(start, end);
            }

            return string;
        }
    }

    private Group currentWorkingGroup;

    private Group currentGroup;

    private StringBuilder group;

    private int currentIndex;

    private Position content;

    private Position tag;

    private Map<String, Position> attributes;

    private List<String> attributePositions;

    public HtmlLineMatcher(final String line) {
        super(line);
        group = new StringBuilder();
    }

    public HtmlLineMatcher(final HtmlLineMatcher other, final String line) {
        super(line);
        currentGroup = other.currentGroup;
        currentWorkingGroup = other.currentWorkingGroup;
        attributePositions = other.attributePositions;
        attributes = other.attributes;
        tag = other.tag;
        content = other.content;
        group = other.group;
    }

    private char next() {
        final char c = getLine().charAt(currentIndex++);
        group.append(c);
        return c;
    }

    private void before() {
        currentIndex--;
        group.deleteCharAt(group.length() - 1);
    }

    private void clean() {
        currentGroup = null;
        currentWorkingGroup = null;
        attributePositions = null;
        attributes = null;
        content = null;
        tag = null;
    }

    private Group getWorkingGroupForTag() {
        final String name = tag.toString();

        // Select the right group according to the name
        for (final Group g : Group.values()) {
            if (name.equals(g.tagName)) {
                return g;
            }
        }

        return null;
    }

    private boolean evaluateEndTag(final boolean inline) {

        // Tag is unknown, let ignore it
        if (currentWorkingGroup == null) {
            tag = null;
            attributes = null;
            attributePositions = new ArrayList<String>();
            return false;
        }

        // Inline tag
        if (currentWorkingGroup.mustBeInline) {
            // Must be inline
            // In that case, tag is not inline or it does not contains the required inline attribute
            if (!inline || !attributes.containsKey(currentWorkingGroup.inlineAttribute)) {
                clean();
                return false;
            }

            return true;
        } else if (currentWorkingGroup.cannotBeInLine) {
            // Must not be inline
            // In that case, the tag is inline while it should not
            if (inline) {
                clean();
            } else {
                // Not inline, read content
                currentWorkingGroup = Group.CONTENT_GROUP;
            }
        } else {
            // Might be inline or not
            // In that case the tag is inline but does not contains the required attribute
            if (inline && !attributes.containsKey(currentWorkingGroup.inlineAttribute)) {
                clean();
            } else if (!inline) {
                // Not inline, read content
                currentWorkingGroup = Group.CONTENT_GROUP;
            }
        }

        return false;
    }

    private boolean readTag() {
        final int start = currentIndex;

        // This flag is true while we read the tag name
        boolean read = true;

        while (currentIndex < getLine().length()) {
            final char c = next();

            // Tag name has been read
            if (c == ' ' && read) {
                tag = new Position(start, currentIndex - 1);
                read = false;
                currentWorkingGroup = getWorkingGroupForTag();
            } else if (!read) {
                final int end = currentIndex - 1;
                final AtomicBoolean inline = new AtomicBoolean();

                if (endTag(inline)) {
                    // End of the tag is reached
                    tag = new Position(start, end);
                    currentWorkingGroup = getWorkingGroupForTag();

                    return evaluateEndTag(inline.get());
                } else {
                    // Read the next attribute
                    readAttribute();
                }
            }
        }

        return false;
    }

    private boolean endTag(final AtomicBoolean inline) {
        final int initial = currentIndex;

        // Look for a termination character after 0 to many space characters
        while (currentIndex < getLine().length()) {
            final char c = next();
            boolean other = false;

            // Not a space
            if (c != ' ') {

                // End found
                if (c == '/') {
                    if (currentIndex < getLine().length() - 1 && getLine().charAt(currentIndex + 1) == '>') {
                        currentIndex++;

                        // This is an inline tag
                        if (inline != null) {
                            inline.set(true);
                        }

                        currentGroup = getWorkingGroupForTag();
                        return true;
                    }
                } else if (c == '>') {
                    currentIndex++;
                    currentGroup = getWorkingGroupForTag();
                    return true;
                } else {
                    other = true;
                }
            } else {
                other = true;
            }

            if (other) {
                // Something needs to be read before the end of the tag
                group.delete(initial, group.length());
                currentIndex = initial;
                break;
            }
        }

        return false;
    }

    private void readAttribute() {
        int start = currentIndex - 1;
        boolean readName = true;

        if (attributes == null) {
            attributes = new HashMap<String, Position>();
            attributePositions = new ArrayList<String>();
        }

        while (currentIndex < getLine().length()) {
            final char c = next();

            if (c != ' ') {
                // Spaces have been read until the beginning of the attribute name
                if (!readName) {
                    readName = true;
                    start = currentIndex - 1;
                } else if (c == '=') {
                    // A value exists for the attribute
                    final String key = getLine().substring(start, currentIndex - 1);
                    attributes.put(key, new Position(start, readValue()));
                    attributePositions.add(key);
                    return;
                } else {
                    before();

                    if (endTag(null)) {
                        return;
                    } else {
                        // call to endTag() has reset position
                        next();
                    }
                }
            }
        }
    }

    private int readValue() {
        Character quoteValue = null;
        boolean read = false;

        while (currentIndex < getLine().length()) {
            final char c = next();

            // All spaces have been read until the beginning of the value
            if (!read && c != ' ') {
                read = true;

                // Quotes are around the value
                if (c == '\'' || c == '"') {
                    quoteValue = c;
                }
            } else if (read) {
                // If no quote, the space marks the end of value
                // otherwise the quote marks the end (if not escaped
                if ((quoteValue == null && c == ' ') || (getLine().charAt(currentIndex - 1) != '\\' && c == quoteValue)) {
                    return currentIndex;
                }
            }
        }

        return getLine().length();
    }

    private boolean readComment() {
        final int start = currentIndex;
        final String l = getLine();

        // Comment tag found
        if (currentIndex + NumberUtils.TWO < l.length()
                && l.charAt(currentIndex++) == '!' && l.charAt(currentIndex++) == '-' && l.charAt(currentIndex++) == '-') {
            currentWorkingGroup = Group.COMMENT_GROUP;
            content = new Position(currentIndex, -1);
            group.append("<!--");
            return endComment();
        } else {
            currentIndex = start;
        }

        return false;
    }

    private boolean endComment() {
        int candidate = 0;

        // Looking for the end of comment
        while (currentIndex < getLine().length()) {
            final char c = next();

            switch (candidate) {

                // Test first character in --> token
                case 0:
                    // First character found, mark it
                    if (c == '-') {
                        candidate = 1;
                    }

                    break;
                // Test second character in --> token
                case 1:
                    // Second character found, mark it
                    if (c == '-') {
                        candidate++;
                    } else {
                        // Not the end of the tag: append content
                        candidate--;
                    }

                    break;
                // Test third character in --> token
                case NumberUtils.TWO:
                    // Comment read
                    if (c == '>') {
                        currentGroup = Group.COMMENT_GROUP;
                        content.end = currentIndex;
                        return true;
                    }

                    // End not found
                default:
                    candidate = 0;
            }

            currentIndex++;
        }

        return false;
    }

    private boolean endContent() {
        final String l = getLine();
        int candidate = 0;

        // Looking for the end of content
        final char c = next();

        while (currentIndex < getLine().length()) {
            switch (candidate) {

                // Test first character in </ token
                case 0:
                    // First character found, mark it
                    if (c == '<') {
                        candidate = 1;
                    }

                    break;
                // Test second character in </ token
                case 1:
                    final int next = currentIndex + 1;

                    // Content read
                    if (c == '/' && (next + tag.length() < l.length()
                            && tag.toString().equals(l.substring(next, next + tag.length())))) {
                        currentIndex += tag.length();
                        currentGroup = getWorkingGroupForTag();
                        currentWorkingGroup = null;
                        return true;
                    }

                    // End not found
                default:
                    candidate = 0;
            }

            currentIndex++;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean find() {

        // Currently building a group, continue
        if (currentWorkingGroup != null) {
            switch (currentWorkingGroup) {
                case COMMENT_GROUP:
                    return endComment();
                case CONTENT_GROUP:
                    return endContent();
                default:
                    throw new IllegalStateException();
            }
        }

        clean();

        // Try to find the beginning of a comment or a tag
        while (currentIndex < getLine().length()) {
            final char c = next();

            // Something needs to be checked
            if (c == '<') {
                // A comment or a tag has been read
                if (readComment() || readTag()) {
                    return true;
                }
            }
        }

        return currentGroup != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int start() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int start(final int group) {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int end() {
        final String retval = group();
        return retval == null ? -1 : retval.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int end(final int group) {
        final String retval = group(group);
        return retval == null ? -1 : retval.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String group() {
        return group.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String group(final int groupNumber) {
        // Group available and parameter seems valid
        if (currentGroup != null && groupNumber >= 0) {

            // Not an attribute group
            if (Group.values().length > groupNumber) {
                final Group g = Group.values()[groupNumber];

                // Return content when available, otherwise return entire group
                return (g == Group.COMMENT_GROUP
                        || g == Group.CONTENT_GROUP
                        || (!g.mustBeInline && content != null)) ? content.toString() : group.toString();
            } else if (attributes != null && (groupNumber < Group.values().length + attributePositions.size())) {
                // Return an attribute group
                return String.valueOf(attributes.get(attributePositions.get(groupNumber - Group.values().length)));
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int groupCount() {
        return attributes != null ?
                (Group.values().length + attributes.size() -1) : (currentGroup == null ? 0 : currentGroup.ordinal());
    }
}
