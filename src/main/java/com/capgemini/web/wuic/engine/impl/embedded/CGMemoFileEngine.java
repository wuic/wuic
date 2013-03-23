/*
 * "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * •   The above copyright notice and this permission notice shall be included in
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


package com.capgemini.web.wuic.engine.impl.embedded;

import com.capgemini.web.wuic.WuicResource;
import com.capgemini.web.wuic.configuration.Configuration;
import com.capgemini.web.wuic.engine.Engine;
import com.capgemini.web.wuic.engine.EngineOutputManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * <p>
 * This {@link Engine} is a tool which is useful to remember what are the
 * files which have been parsed and returned by another {@link Engine}. The
 * files are stored in a "memo.txt" file which could be reused when it is
 * necessary to use a set of file that have been previously generated.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.1.1
 */
public class CGMemoFileEngine extends Engine {

    /**
     * The {@link Configuration}.
     */
    private Configuration configuration;
    
    /**
     * <p>
     * Builds a new {@code CGMemoFileEngine} instance.
     * </p>
     * 
     * @param config the {@link Configuration} to be used
     */
    public CGMemoFileEngine(final Configuration config) {
        configuration = config;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<WuicResource> parse(final List<WuicResource> files)
            throws IOException {
        
        // Create a file "memo.txt" in the working directory
        final EngineOutputManager outputManager = EngineOutputManager.getInstance();
        final String directory = outputManager.getWorkingDirectory(configuration);
        final File target = new File(directory, "memo.txt");
        final List<String> lines = new ArrayList<String>(files.size());
        
        // Store each file absolute path
        for (WuicResource resource : files) {
            lines.add(resource.getName());
        }
        
        // Write all lines (overriding existing file)
        FileUtils.writeLines(target, configuration.charset(), lines, Boolean.FALSE);
        
        return files;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return configuration.aggregate() || configuration.compress();
    }
}
