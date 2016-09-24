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


package com.github.wuic.test;

import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.InMemoryInput;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

/**
 * <p>
 * Base class to be extended by test tests.
 * </p>
 * 
 * @author Guillaume DROUET
 * @since 0.3.1
 */
public class WuicTest {

    /**
     * <p>
     * Writes on the disk the given nut if the system property 'wuic.test.storeTo' is set.
     * Useful to check if generated files are correct.
     * </p>
     *
     * @param name the path name on the disk
     * @param nut the nut
     * @throws Exception if an I/O error occurs
     */
    protected void writeToDisk(final Nut nut, final String name) throws Exception {
        final String dir = System.getProperty("wuic.test.storeTo");

        if (dir != null) {
            IOUtils.copyStream(nut.openStream().inputStream(), new FileOutputStream(new File(dir, name)));
        }
    }

    /**
     * <p>
     * Returns an answer to mock {@link com.github.wuic.nut.Nut#openStream()} that produces an {@link com.github.wuic.util.Input}
     * reading the given {@code String}.
     * </p>
     *
     * @param content the {@code String} read by the input
     * @return the answer
     */
    public static Answer<Object> openStreamAnswer(final String content) {
        return new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return new InMemoryInput(content, Charset.defaultCharset().displayName());
            }
        };
    }
}
