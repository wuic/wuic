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

import com.github.wuic.configuration.Configuration;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * <p>
 * This class is in charge to provides directory where the {@link Engine engines}
 * must generate their files.
 * </p>
 * 
 * <p>
 * Actually, this directory is located in the temporary directory of the file system
 * and in a directory name 'wuic' ({@link EngineOutputManager#BASE_WORKING_NAME}).
 * The manager also clears the directories tree when it is created.
 * </p>
 * 
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.1.0
 */
public final class EngineOutputManager {

    /**
     * The working directory name created in the temporary directory.
     */
    public static final String BASE_WORKING_NAME = "wuic-" + System.currentTimeMillis();
  
    /**
     * The unique instance.
     */
    private static EngineOutputManager instance = null;
    
    /**
     * <p>
     * Builds the instance. Firstly clears the working directory.
     * </p> 
     * 
     * @throws IOException if an I/O error occurs while clearing the working directory
     */
    private EngineOutputManager() throws IOException {
        
        // Build the absolute working directory path
        final StringBuilder builder = new StringBuilder();
        builder.append(System.getProperty("java.io.tmpdir"));
        builder.append(System.getProperty("file.separator"));
        builder.append(BASE_WORKING_NAME);
        
        // Deletes it from the file system
        FileUtils.deleteDirectory(new File(builder.toString()));
    }
    
    /**
     * <p>
     * Gets the unique instance of the {@code EngineOutputManager}.
     * </p>
     * 
     * @return the unique instance
     * @throws IOException if an I/O error occurs when the instance is created at the first call
     */
    public static EngineOutputManager getInstance() throws IOException {
        if (instance == null) {
            instance = new EngineOutputManager();
        }
        
        return instance;
    }

    /**
     * <p>
     * Initializes the working directory for the given {@link Configuration}. If
     * the directory already exists, then it will be cleared.
     * </p>
     * 
     * @param configuration the configuration to use
     * @param filter the filter that indicates the files that should not be deleted (could be null)
     * @return the absolute path
     * @throws IOException if an I/O error occurs when initializing the directory
     */
    public String initWorkingDirectory(final Configuration configuration,
            final FilenameFilter filter) throws IOException {
        
        // Make sure the path exists on the file system before returning it
        final File retval = new File(getWorkingDirectory(configuration));
        
        if (retval.exists()) {
            recursiveDelete(retval, filter);
        }
        
        retval.mkdirs();
        
        return retval.getAbsolutePath();
    }

    /**
     * <p>
     * Delete recursively all the files contained in a specified root directory.
     * </p>
     * 
     * @param file the root directory
     * @param filter the filter that indicates the files that should not be deleted (could be null)
     */
    private void recursiveDelete(final File file, final FilenameFilter filter) {
        if (file.isDirectory()) {
            final File[] files = filter != null ? file.listFiles(filter) : file.listFiles();
            
            for (File toDelete : files) {
                recursiveDelete(toDelete, filter);
            }
        }
        
        file.delete();
    }
    
    /**
     * <p>
     * Returns the working directory for files to be generated using the given
     * {@link Configuration}. An unique path for each configuration is generated
     * using their {@code hashChode()} method.
     * </p>
     * 
     * @param configuration the configuration to use
     * @return the absolute path
     */
    public String getWorkingDirectory(final Configuration configuration) {
        // Builds the path
        final StringBuilder builder = new StringBuilder();
        final String separator = System.getProperty("file.separator"); 
        builder.append(System.getProperty("java.io.tmpdir"));
        builder.append(separator);
        builder.append(BASE_WORKING_NAME);
        builder.append(separator);

        // Keep the value positive to delete the '-' character from the path
        final int hash = configuration.hashCode();
        builder.append(hash < 0 ? hash * -1 : hash);
        
        builder.append(separator);
        
        return builder.toString();
    }
    
    /**
     * <p>
     * Returns all the files generated in the directory dedicated to the given
     * {@link Configuration}. The methods used the memo.txt file generated by the
     * {@link com.github.wuic.engine.impl.embedded.CGMemoFileEngine}.
     * </p>
     * 
     * @param configuration the {@link Configuration}
     * @return the files list, {@code null} if the working directory has not been created yet
     * @throws IOException if an I/O error occurs
     */
    /*public List<WuicResource> getFilesGeneratedFor(final Configuration configuration) throws IOException {
        final File memoFile = new File(getWorkingDirectory(configuration), "memo.txt");
        
        if (memoFile.exists()) {
            final List<String> files = FileUtils.readLines(memoFile, configuration.charset());
            final List<WuicResource> retval = new ArrayList<WuicResource>(files.size());
            
            for (String file : files) {
                retval.add(new FileWuicResource(getWorkingDirectory(configuration), file));
            }
            
            return retval;
        } else {
            return null;
        }
    }*/
}
