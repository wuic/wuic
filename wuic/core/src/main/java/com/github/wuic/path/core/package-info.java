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

/**
 * <p>
 * This package defines core implementation of the path API to resolve a hierarchical path with each element
 * corresponding to either a path, a directory, a ZIP archive or an entry of a ZIP archive.
 * </p>
 *
 * <p>
 * For instance, the aim is to access a path in /foo/lib.jar/statics/fwk.zip/path.js where :
 * <ul>
 *     <li>/foo is a directory</li>
 *     <li>lib.jar is a JAR path</li>
 *     <li>statics is a directory entry inside lib.jar</li>
 *     <li>statics/fwk.zip is a path entry inside lib.jar which is itself a ZIP archive</li>
 *     <li>path.js is a path stored as a path entry inside fwk.zip</li>
 * </ul>
 * </p>
 *
 * <p>
 * See this {@link com.github.wuic.util.IOUtils#buildPath(String) helper} to build a path.
 * </p>
 *
 * @author Guillaume DROUET
 */
package com.github.wuic.path.core;