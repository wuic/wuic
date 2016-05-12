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


package com.github.wuic.engine;

import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.util.NumberUtils;
import com.github.wuic.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Base class for script inspection. A script contains comments (starting with /* or //) that subclasses can ask to
 * ignore when inspecting the content. Regarding the {@link ScriptMatchCondition} used, the class will call the
 * {@link #doFind(char[], int, int, EngineRequest, CompositeNut.CompositeInputStream, ConvertibleNut)} with a char array
 * corresponding to the proper content to inspect. The class relies heavily on char arrays and never create {@code String}
 * object to guarantee a minimal memory footprint.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
public abstract class ScriptLineInspector extends LineInspector {

    /**
     * <p>
     * An enumeration of inspection options.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    public enum ScriptMatchCondition {

        /**
         * Inspects all the content.
         */
        ALL,

        /**
         * Inspects single line comments only.
         */
        SINGLE_LINE_COMMENT,

        /**
         * Inspects multi-line comments only.
         */
        MULTI_LINE_COMMENT,

        /**
         * Inspects the content except comments.
         */
        NO_COMMENT,
    }

    /**
     * Configured conditions.
     */
    private ScriptMatchCondition matchCondition;

    /**
     * Currently inspecting a single line comment.
     */
    private boolean inSingleLineComment;

    /**
     * Currently inspecting multiple line comment.
     */
    private boolean inMultiLineComment;

    /**
     * Currently inspecting a string literal.
     */
    private Character quoteCharacter;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param matchCondition the match configuration
     */
    public ScriptLineInspector(final ScriptMatchCondition matchCondition) {
        this.matchCondition = matchCondition;
        this.inSingleLineComment = false;
        this.inMultiLineComment = false;
        this.quoteCharacter = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void newInspection() {
        inSingleLineComment = false;
        inMultiLineComment = false;
        quoteCharacter = null;
    }

    /**
     * <p>
     * Creates a new {@link ScriptLineInspector} with specified {@link ScriptMatchCondition} that wraps the given
     * inspector and delegate to this object inspection of matched data.
     * </p>
     *
     * @param inspector the inspector
     * @param condition match condition
     * @return the line inspector
     */
    public static LineInspector wrap(final LineInspector inspector,
                                     final ScriptMatchCondition condition) {
        return new Wrapper(condition, inspector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inspect(final LineInspectorListener listener,
                        final char[] data,
                        final int begin,
                        final int length,
                        final EngineRequest request,
                        final CompositeNut.CompositeInputStream cis,
                        final ConvertibleNut originalNut) throws WuicException {
        Range dataRange;
        int start = begin;

        // Iterate on all ranges to inspect in the given chunk of data
        while ((dataRange = findInspectRange(data, start, length - 1)) != null) {

            // Just continue the loop with a new iteration
            if (Range.Delimiter.CONTINUE == dataRange.getDelimiter()) {
                start = dataRange.getStart();
                continue;
            }

            int offset = dataRange.getStart();
            Range range;
            int len;

            // Iterate on all matched range of characters for the current range of data to inspect
            while ((len = dataRange.getEnd() - offset + 1) > 0 && (range = doFind(data, offset, len, request, cis, originalNut)) != null) {
                // The length of found data
                final int findLen = range.getEnd() - range.getStart();
                final List<AppendedTransformation> transformations =
                        appendTransformation(data, range.getStart(), findLen, request, cis, originalNut);

                if (transformations != null) {
                    for (final AppendedTransformation t : transformations) {
                        listener.onMatch(data, t.getStart(), t.getEnd() - t.getStart(), t.getReplacement(), t.getResult());
                    }
                }

                // Update the range of characters to inspect in the next iteration
                offset = range.getEnd();
            }

            start = dataRange.getEnd() + 1;
        }
    }

    /**
     * <p>
     * Finds in the given chunk of data the range of characters to inspect regarding the configured
     * {@link ScriptMatchCondition} and the current state of the inspector (inside a comment or not).
     * </p>
     *
     * @param data char array containing data to inspect
     * @param dataStart where to start looking for the range
     * @param dataEnd where to stop looking for the range
     * @return the range to inspect, {@code null} if no inspection should be performed for ths given chunk of data
     */
    private Range findInspectRange(final char[] data, final int dataStart, final int dataEnd) {
        // No data to inspect
        if (dataStart >= dataEnd) {
            return null;
        }

        final Range range;

        switch (matchCondition) {

            // Match data anywhere
            case ALL:
                range = new Range(Range.Delimiter.EOF, dataStart, dataEnd);
                break;

            // Match data outside comments
            case NO_COMMENT:
                range = findNoCommentInspectRange(data, dataStart, dataEnd);
                break;

            // Match data inside multiple line comment only
            case MULTI_LINE_COMMENT:
                range = multiLineCommentCase(data, dataStart, dataEnd);
                break;

            // Match data inside single line comment only
            case SINGLE_LINE_COMMENT:
                range = singleLineCommentCase(data, dataStart, dataEnd);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return range;
    }

    /**
     * <p>
     * Finds in the given chunk of data the range of characters to inspect for {@link ScriptMatchCondition#NO_COMMENT} case.
     * The method deals with the fact that {@link #noCommentCase(char[], int, int)} return chunks of data delimiter by
     * comments and string literals. This methods merges the sequence in one range and clears the characters from comments
     * in the given char array.
     * </p>
     *
     * @param data char array containing data to inspect
     * @param dataStart where to start looking for the range
     * @param dataEnd where to stop looking for the range
     * @return the range to inspect, {@code null} if no inspection should be performed for ths given chunk of data
     */
    private Range findNoCommentInspectRange(final char[] data, final int dataStart, final int dataEnd) {
        Range r = noCommentCase(data, dataStart, dataEnd);
        final Range range = r;

        // While we don't reach a new comment or EOF, extend the chunk
        while (r != null && r.getDelimiter() != Range.Delimiter.EOF) {
            StringUtils.replace(data, range.getEnd() + 1, r.getStart(), ' ');

            // Update end index
            range.setEnd(r.getEnd());

            r = noCommentCase(data, r.getEnd() + 1, dataEnd);
        }

        // EOF reached
        if (r != null) {
            range.setEnd(r.getEnd());
        }

        return range;
    }

    /**
     * <p>
     * Indicates if start of single line comment is the nearest node.
     * </p>
     *
     * @param startOfSingleLineComment the starting index of single line comment
     * @param startOfMultiLineComment the starting index of multiple line comment
     * @param startOfStringLiteral the starting index of string literal
     * @return {@code true} if the nearest node is a single line comment
     */
    private boolean decideStartOfSingleLineComment(final int startOfSingleLineComment,
                                                   final int startOfMultiLineComment,
                                                   final int startOfStringLiteral) {
        return (startOfSingleLineComment < startOfMultiLineComment || startOfMultiLineComment == -1)
                && (startOfSingleLineComment < startOfStringLiteral || startOfStringLiteral == -1);
    }

    /**
     * <p>
     * Indicates if start of multiple line comment is the nearest node.
     * </p>
     *
     * @param startOfSingleLineComment the starting index of single line comment
     * @param startOfMultiLineComment the starting index of multiple line comment
     * @param startOfStringLiteral the starting index of string literal
     * @return {@code true} if the nearest node is a multiple line comment
     */
    private boolean decideMultipleOfSingleLineComment(final int startOfSingleLineComment,
                                                     final int startOfMultiLineComment,
                                                     final int startOfStringLiteral) {
        return (startOfMultiLineComment < startOfSingleLineComment || startOfSingleLineComment == -1)
                && (startOfMultiLineComment < startOfStringLiteral || startOfStringLiteral == -1);
    }

    /**
     * <p>
     * Indicates if start of string literal the nearest node.
     * </p>
     *
     * @param startOfSingleLineComment the starting index of single line comment
     * @param startOfMultiLineComment the starting index of multiple line comment
     * @param startOfStringLiteral the starting index of string literal
     * @return {@code true} if the nearest node is a string literal
     */
    private boolean decideStringLiteral(final int startOfSingleLineComment,
                                        final int startOfMultiLineComment,
                                        final int startOfStringLiteral) {
        return (startOfStringLiteral < startOfMultiLineComment || startOfMultiLineComment == -1)
                && (startOfStringLiteral < startOfSingleLineComment || startOfSingleLineComment == -1);
    }

    /**
     * <p>
     * Decides what is the nearest node regarding their starting index (single line comment, multiple line comment or
     * string literal) and adapts the stats of this object.
     * </p>
     *
     * @param startOfSingleLineComment the starting index of single line comment
     * @param startOfMultiLineComment the starting index of multiple line comment
     * @param startOfStringLiteral the starting index of string literal
     * @return a range with minimal valid index as start and end index, {@code null} otherwise
     */
    private Range decide(final int startOfSingleLineComment, final int startOfMultiLineComment, final int startOfStringLiteral) {
        // In multiple line comment
        if (startOfMultiLineComment != -1
                && decideMultipleOfSingleLineComment(startOfSingleLineComment, startOfMultiLineComment, startOfStringLiteral)) {
            inMultiLineComment = true;
            quoteCharacter = null;
            return new Range(Range.Delimiter.START_MULTIPLE_LINE_OF_COMMENT, startOfMultiLineComment, startOfMultiLineComment);
        // In single line comment
        } else if (startOfSingleLineComment != -1 &&
                decideStartOfSingleLineComment(startOfSingleLineComment, startOfMultiLineComment, startOfStringLiteral)) {
            inSingleLineComment = true;
            quoteCharacter = null;
            return new Range(Range.Delimiter.START_SINGLE_LINE_OF_COMMENT, startOfSingleLineComment, startOfSingleLineComment);
        // In a string literal
        } else if (startOfStringLiteral != -1
                && decideStringLiteral(startOfSingleLineComment, startOfMultiLineComment, startOfStringLiteral)) {
            return new Range(Range.Delimiter.START_OF_LITERAL, startOfStringLiteral, startOfStringLiteral);
        }

        return null;
    }

    /**
     * <p>
     * Looking for the next range to inspect when {@code matchCondition == ScriptMatchCondition.SINGLE_LINE_COMMENT}.
     * </p>
     *
     * @param data char array containing data to inspect
     * @param dataStart where to start looking for the range
     * @param dataEnd where to stop looking for the range
     * @return the range to inspect, {@code null} if no inspection should be performed for ths given chunk of data
     */
    private Range singleLineCommentCase(final char[] data, final int dataStart, final int dataEnd) {
        final Range range;

        // Currently in a string literal
        if (quoteCharacter != null) {
            // Looking for the end of string literal: ignore the first character that is reserved for literal opening
            final int end = findEndOfStringLiteral(data, dataStart + 1, dataEnd);
            range = end != -1 ? new Range(Range.Delimiter.CONTINUE, end + 1, dataEnd) : null;
        // Currently inside a multiple line comment
        } else if (inMultiLineComment) {
            // Looking for the end of comment: ignore the two first characters that are reserved for comment opening
            final int end = findEndOfMultiLineComment(data, dataStart + NumberUtils.TWO, dataEnd);

            // End of comment found, look for the next valid range
            if (end != -1) {
                inMultiLineComment = false;
                range = new Range(Range.Delimiter.CONTINUE, end + 1, dataEnd);
            } else {
                // Still inside the comment: ignore
                range = null;
            }
        // Already in a single line comment
        } else if (inSingleLineComment) {
            // Looking for the end of comment, that will be the end of the range
            final int endOfSingleLineComment = findEndOfSingleLineComment(data, dataStart, dataEnd);

            // End of comment found, create range
            if (endOfSingleLineComment != -1) {
                inSingleLineComment = false;
                range = new Range(Range.Delimiter.END_SINGLE_LINE_OF_COMMENT, dataStart, endOfSingleLineComment);
            } else {
                // Otherwise all the content be inspected
                range = new Range(Range.Delimiter.EOF, dataStart, dataEnd);
            }
        } else {
            // Looking for the beginning of a comment or string literal
            final Range decision = decide(findStartOfSingleLineComment(data, dataStart, dataEnd),
                    findStartOfMultiLineComment(data, dataStart, dataEnd),
                    findStartOfStringLiteral(data, dataStart, dataEnd));

            // No comment or string literal found: nothing to inspect
            if (decision == null) {
                range = null;
            } else {
                range = new Range(Range.Delimiter.CONTINUE, decision.getStart(), dataEnd);
            }
        }

        return range;
    }

    /**
     * <p>
     * Looking for the next range to inspect when {@code matchCondition == ScriptMatchCondition.MULTI_LINE_COMMENT}.
     * </p>
     *
     * @param data char array containing data to inspect
     * @param dataStart where to start looking for the range
     * @param dataEnd where to stop looking for the range
     * @return the range to inspect, {@code null} if no inspection should be performed for ths given chunk of data
     */
    private Range multiLineCommentCase(final char[] data, final int dataStart, final int dataEnd) {
        final Range range;

        // Currently in a string literal
        if (quoteCharacter != null) {
            // Looking for the end of string literal: ignore the first character that is reserved for literal opening
            final int end = findEndOfStringLiteral(data, dataStart + 1, dataEnd);
            range = end != -1 ? new Range(Range.Delimiter.CONTINUE, end + 1, dataEnd) : null;
        // Currently inside a multiple line comment
        } else if (inMultiLineComment) {
            // Looking for the end of comment: ignore the two first characters that are reserved for comment opening
            final int end = findEndOfMultiLineComment(data, dataStart + NumberUtils.TWO, dataEnd);

            if (end != -1) {
                inMultiLineComment = false;
                range = new Range(Range.Delimiter.END_MULTIPLE_LINE_OF_COMMENT, dataStart, end);
            } else {
                range = new Range(Range.Delimiter.EOF, dataStart, dataEnd);
            }
        } else if (inSingleLineComment) {
            final int endOfSingleLineComment = findEndOfSingleLineComment(data, dataStart, dataEnd);

            if (endOfSingleLineComment != -1) {
                inSingleLineComment = false;
                range = new Range(Range.Delimiter.CONTINUE, endOfSingleLineComment + 1, dataEnd);
            } else {
                range = null;
            }
        }  else {
            // Looking for the beginning of a comment or string literal
            final Range decision = decide(findStartOfSingleLineComment(data, dataStart, dataEnd),
                    findStartOfMultiLineComment(data, dataStart, dataEnd),
                    findStartOfStringLiteral(data, dataStart, dataEnd));

            // No comment or string literal found: nothing to inspect
            if (decision == null) {
                range = null;
            } else {
                range = new Range(Range.Delimiter.CONTINUE, decision.getStart(), dataEnd);
            }
        }

        return range;
    }

    /**
     * <p>
     * Looking for the next range to inspect when {@code matchCondition == ScriptMatchCondition.NO_COMMENT}.
     * </p>
     *
     * @param data char array containing data to inspect
     * @param dataStart where to start looking for the range
     * @param dataEnd where to stop looking for the range
     * @return the range to inspect, {@code null} if no inspection should be performed for ths given chunk of data
     */
    private Range noCommentCase(final char[] data, final int dataStart, final int dataEnd) {
        final Range range;
        final int start;

        // We are already in a string literal
        if (quoteCharacter != null) {
            // Looking for the end of string literal: ignore the first character that is reserved for literal opening
            final int end = findEndOfStringLiteral(data, dataStart + 1, dataEnd);
            return end == -1 ? null : new Range(Range.Delimiter.END_OF_LITERAL, dataStart, end);
        // else if we are already inside a comment, try to find the end of it
        } else if (inMultiLineComment) {
            start = findEndOfMultiLineComment(data, dataStart + NumberUtils.TWO, dataEnd);
        } else if (inSingleLineComment) {
            start = findEndOfSingleLineComment(data, dataStart, dataEnd);
        } else {
            // Looking for the beginning of a comment or string literal
            final int startOfSingleLineComment = findStartOfSingleLineComment(data, dataStart, dataEnd);
            final int startOfMultiLineComment = findStartOfMultiLineComment(data, dataStart, dataEnd);
            final int startOfLiteral = findStartOfStringLiteral(data, dataStart, dataEnd);
            final Range decision = decide(startOfSingleLineComment, startOfMultiLineComment, startOfLiteral);

            // No comment or literal detected, cover the entire range
            if (decision == null) {
                range = new Range(Range.Delimiter.EOF, dataStart, dataEnd);
            } else {
                // Comment or literal detected, cover the entire range
                range = new Range(decision.getDelimiter(), dataStart, decision.getStart() - 1);
            }

            return range;
        }

        // The entire buffer is inside a comment: nothing to inspect
        if (start == -1) {
            range = null;
        } else {
            // End of the comment found, look for the next range
            inMultiLineComment = false;
            inSingleLineComment = false;
            range = noCommentCase(data, start + 1, dataEnd);
        }

        return range;
    }

    /**
     * <p>
     * Finds in the given chunk of data (delimited by te specified offset and length) the range of the first occurrence
     * of matching text. To inspect the given char array, a {@code String} can be easily created with the parameter like
     * that: {@code new String(buffer, offset, length)}.
     * </p>
     *
     * @param buffer the char array containing data to inspect
     * @param offset the offset to use in the buffer
     * @param length the number of characters to inspect
     * @param request the request that initiated the inspection
     * @param cis the composite stream that is currently inspected if any
     * @param originalNut the nut that procduces the inspected stream
     * @return the range of matched data in the buffer
     */
    public abstract Range doFind(char[] buffer,
                                 int offset,
                                 int length,
                                 EngineRequest request,
                                 CompositeNut.CompositeInputStream cis,
                                 ConvertibleNut originalNut);

    /**
     * <p>
     * Computes the replacement to be made inside the text for the given {@code Matcher} which its {@code find()}
     * method as just been called. To inspect the given char array that match the data, a {@code String} can be easily
     * created with the parameter like that: {@code new String(buffer, offset, length)}.
     * </p>
     *
     * @param buffer the char array containing the matched data
     * @param offset the offset to use in the buffer
     * @param length the number of characters to inspect
     * @param request the request that orders this transformation
     * @param cis a composite stream which indicates what nut owns the transformed text, {@code null} if the nut is not a composition
     * @param originalNut the original nut
     * @return all the appended transformations
     * @throws com.github.wuic.exception.WuicException if an exception occurs
     */
    protected abstract List<AppendedTransformation> appendTransformation(char[] buffer,
                                                                         int offset,
                                                                         int length,
                                                                         EngineRequest request,
                                                                         CompositeNut.CompositeInputStream cis,
                                                                         ConvertibleNut originalNut) throws WuicException;

    /**
     * <p>
     * Finds the beginning of a string literal (single quote or double quote character) that is not escaped with a
     * backslash character.
     * </p>
     *
     * @param buffer the buffer
     * @param start first index in buffer
     * @param end last index in buffer
     * @return the index of first quote occurrence, -1 if nothing is found
     */
    private int findStartOfStringLiteral(final char[] buffer, final int start, final int end) {
        // Looking for the single or double quote character
        for (int i = start; i < end; i++) {
            final char quote = buffer[i];

            if (quote == '\'' || quote == '"') {
                boolean escaped = false;

                for (int j = i - 1; j >= start && buffer[j] == '\\'; j--) {
                    escaped = !escaped;
                }

                if (!escaped) {
                    quoteCharacter = quote;
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * <p>
     * Finds the beginning of single line comment ('{@code //}') in the specified char array.
     * </p>
     *
     * @param buffer the buffer
     * @param start first index in buffer
     * @param end last index in buffer
     * @return the index of first {@code //} occurrence, -1 if nothing is found
     */
    private int findStartOfSingleLineComment(final char[] buffer, final int start, final int end) {
        // Looking for the '/' character
        for (int i = start; i < end; i++) {
            if (i < buffer.length -1 && buffer[i] == '/' && buffer[i + 1] == '/') {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * Finds the beginning of multiple line comment ('{@code /*}') in the specified char array.
     * </p>
     *
     * @param buffer the buffer
     * @param start first index in buffer
     * @param end last index in buffer
     * @return the index of first {@code /*} occurrence, -1 if nothing is found
     */
    private int findStartOfMultiLineComment(final char[] buffer, final int start, final int end) {
        // Looking for '/' and '*' characters
        for (int i = start; i < end; i++) {
            if (i < end - 1 && buffer[i] == '/' && buffer[i + 1] == '*') {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * Finds the end of a string literal that equals to {@link #quoteCharacter} and that is not
     * escaped with a backslash character.
     * </p>
     *
     * @param buffer the buffer
     * @param start first index in buffer
     * @param end last index in buffer
     * @return the index of first quote occurrence, -1 if nothing is found
     */
    private int findEndOfStringLiteral(final char[] buffer, final int start, final int end) {
        // Looking for the quoteCharacter
        for (int i = start; i < end; i++) {
            final char quote = buffer[i];

            if (quoteCharacter.equals(quote)) {
                boolean escaped = false;

                for (int j = i - 1; j >= start && buffer[j] == '\\'; j--) {
                    escaped = !escaped;
                }

                if (!escaped) {
                    quoteCharacter = null;
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * <p>
     * Finds the end of multiple line comment ('{@code *}{@code /}') in the specified char array.
     * </p>
     *
     * @param buffer the buffer
     * @param start first index in buffer
     * @param end last index in buffer
     * @return the index of first {@code *}{@code /} occurrence, -1 if nothing is found
     */
    private int findEndOfMultiLineComment(final char[] buffer, final int start, final int end) {
        // Looking for '*' and '/' characters
        for (int i = start; i < end; i++) {
            if (i > start && buffer[i] == '/' && buffer[i - 1] == '*') {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * Finds the end of single line comment ({@code '\n'}) in the specified char array.
     * </p>
     *
     * @param buffer the buffer
     * @param start first index in buffer
     * @param end last index in buffer
     * @return the index of first {@code \n} occurrence, -1 if nothing is found
     */
    private int findEndOfSingleLineComment(final char[] buffer, final int start, final int end) {
        // Looking for \n character
        for (int i = start; i < end; i++) {
            if (buffer[i] == '\n') {
                return i;
            }
        }

        return -1;
    }

    /**
     * <p>
     * Represents a range with a start and an end position.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    protected static class Range {

        /**
         * <p>
         * Enumeration of possible delimiters between range of characters.
         * </p>
         *
         * @author Guillaume DROUET
         * @since 0.5.3
         */
        public enum Delimiter {

            /**
             * Beginning of a comment on a single line.
             */
            START_SINGLE_LINE_OF_COMMENT,

            /**
             * Beginning of a comment on multiple lines.
             */
            START_MULTIPLE_LINE_OF_COMMENT,

            /**
             * Start of literal.
             */
            START_OF_LITERAL,

            /**
             * Beginning of a comment on a single line.
             */
            END_SINGLE_LINE_OF_COMMENT,

            /**
             * End of a comment on multiple lines.
             */
            END_MULTIPLE_LINE_OF_COMMENT,

            /**
             * End of literal.
             */
            END_OF_LITERAL,

            /**
             * End of char array reached.
             */
            EOF,

            /**
             * This range just needs to be use to launch a new inspection, whatever it represents.
             * This allows to reduce the number of recursive calls and avoid StackOverflow errors in very large files.
             */
            CONTINUE,
        }

        /**
         * Delimiter.
         */
        private final Delimiter delimiter;

        /**
         * Start position.
         */
        private int start;

        /**
         * End position.
         */
        private int end;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param delimiter the delimiter
         * @param start the start position
         * @param end the ebd position
         */
        public Range(final Delimiter delimiter, final int start, final int end) {
            this.delimiter = delimiter;
            this.start = start;
            this.end = end;
        }

        /**
         * <p>
         * Gets the delimiter.
         * </p>
         *
         * @return the delimiter
         */
        public Delimiter getDelimiter() {
            return delimiter;
        }

        /**
         * <p>
         * Gets the end position.
         * </p>
         *
         * @return the end position
         */
        public int getEnd() {
            return end;
        }

        /**
         * <p>
         * Sets the end position.
         * </p>
         *
         * @param end the end position
         */
        public void setEnd(final int end) {
            this.end = end;
        }

        /**
         * <p>
         * Gets the start position.
         * </p>
         *
         * @return the start position
         */
        public int getStart() {
            return start;
        }

        /**
         * <p>
         * Sets the start position.
         * </p>
         *
         * @param start the start position
         */
        public void setStart(final int start) {
            this.start = start;
        }
    }

    /**
     * <p>
     * Wraps a {@link LineInspector} and delegates inspections to each matched range according to a configured
     * {@link ScriptMatchCondition}.
     * </p>
     *
     * @author Guillaume DROUET
     * @since 0.5.3
     */
    private static final class Wrapper extends ScriptLineInspector {

        /**
         * Wrapped inspector
         */
        private final LineInspector wrap;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param matchCondition the match condition
         * @param wrap the wrapper inspector
         */
        private Wrapper(final ScriptMatchCondition matchCondition, final LineInspector wrap) {
            super(matchCondition);
            this.wrap = wrap;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Range doFind(final char[] buffer,
                            final int offset,
                            final int length,
                            final EngineRequest request,
                            final CompositeNut.CompositeInputStream cis,
                            final ConvertibleNut originalNut) {
            return new Range(Range.Delimiter.EOF, offset, offset + length);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<AppendedTransformation> appendTransformation(final char[] buffer,
                                                                    final int offset,
                                                                    final int length,
                                                                    final EngineRequest request,
                                                                    final CompositeNut.CompositeInputStream cis,
                                                                    final ConvertibleNut originalNut)
                throws WuicException {
            final List<AppendedTransformation> retval = new ArrayList<AppendedTransformation>();

            // Captures all the matched data
            wrap.inspect(new LineInspectorListener() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void onMatch(final char[] data,
                                    final int offset,
                                    final int length,
                                    final String replacement,
                                    final List<? extends ConvertibleNut> extracted)
                        throws WuicException {
                    retval.add(new AppendedTransformation(offset, offset + length, extracted, replacement));
                }
            }, buffer, offset, length, request, cis, originalNut);

            return retval;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String toString(final ConvertibleNut convertibleNut) throws IOException {
            return wrap.toString(convertibleNut);
        }
    }
}