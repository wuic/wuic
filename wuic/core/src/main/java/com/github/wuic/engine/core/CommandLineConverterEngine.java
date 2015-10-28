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

import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.config.BooleanConfigParam;
import com.github.wuic.config.ConfigConstructor;
import com.github.wuic.config.StringConfigParam;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineService;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ByteArrayNut;
import com.github.wuic.nut.CompositeNut;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.SourceMapNutImpl;
import com.github.wuic.util.BiFunction;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutDiskStore;
import com.github.wuic.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * <p>
 * This {@link com.github.wuic.engine.Engine} is able to {@link com.github.wuic.engine.EngineType#CONVERTER convert}
 * nuts by executing a command line. A command line is a {@code String} representing a pattern to be transformed in order
 * to obtain a command to execute according to the context. The context contains specifically three mandatory information
 * that must be identified in the command line pattern:
 *   <ul>
 *     <li>The paths of files to be processed. They are identified thanks to the {@link #PATH_TOKEN} token.</li>
 *     <li>The result path. It is identified thanks to the {@link #OUT_PATH_TOKEN} token.</li>
 *     <li>The source map path. It is identified thanks to the {@link #SOURCE_MAP_TOKEN} token.</li>
 *   </ul>
 *
 * Optionally, a {@link #BASE_PATH_TOKEN} token can be specified to allow the engine to pass all the paths via the
 * {@link #PATH_TOKEN} token relatively to the path specified via the {@link #BASE_PATH_TOKEN} token.
 * </p>
 *
 * <p>
 * For instance, the following command line pattern is valid: {@code my-cmd --in %paths% --out %out_path% --sourcemap %sourceMap%}.
 * According to the processed nuts, the pattern can be used to execute a command like that:
 * {@code my-cmd --in /path/foo.less /path/bar.less --out aggregate.css --sourcemap aggregate.css.map}.
 * Optionally, you can specify the base path if possible to obtain the same result as above:
 *   <ul>
 *     <li>The pattern {@code my-cmd --b %basePath% --in %paths% --out %out_path% --sourcemap %sourceMap%}...</li>
 *     <li>... gives {@code my-cmd --b /path --in foo.less bar.less --out aggregate.css --sourcemap aggregate.css.map}.</li>
 *   </ul>
 * </p>
 *
 * <p>
 * By default the engine will use a space character as path separator in the command line. You can change it by setting
 * the attribute {@link ApplicationConfig#PATH_SEPARATOR}. For instance you may obtain a command line formatted like this
 * by default: {@code my-cmd --b /path --in foo.less bar.less --out aggregate.css --sourcemap aggregate.css.map}. If you
 * specify the value "," for the {@link ApplicationConfig#PATH_SEPARATOR} setting, the executed command will look like
 * this: {@code my-cmd --b /path --in foo.less,bar.less --out aggregate.css --sourcemap aggregate.css.map}.
 * </p>
 *
 * <p>
 * Some mandatory settings must be used to configure this engine if you want to activate it:
 *   <ul>
 *     <li>{@link ApplicationConfig#INPUT_NUT_TYPE}: the {@link NutType} name corresponding to the type expected of input file</li>
 *     <li>{@link ApplicationConfig#OUTPUT_NUT_TYPE}: the {@link NutType} name corresponding to the type of expected output file</li>
 *   </ul>
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.3
 */
@EngineService(injectDefaultToWorkflow = true)
public class CommandLineConverterEngine extends AbstractConverterEngine
        implements BiFunction<List<String>, File, Boolean> {

    /**
     * The base path token in the command line pattern.
     */
    public static final String BASE_PATH_TOKEN = "%basePath%";

    /**
     * Path  token in the command line pattern
     */
    public static final String PATH_TOKEN = "%paths%";

    /**
     * Source map token in the command line pattern.
     */
    public static final String SOURCE_MAP_TOKEN = "%sourceMap%";

    /**
     * Output path token in the command line pattern.
     */
    public static final String OUT_PATH_TOKEN = "%outPath%";

    /**
     * Error message if pattern does not contains a mandatory token.
     */
    public static final String ERROR_MESSAGE = "Command '%s' and/or libraries %s must contains the token '%s'";

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(CommandLineConverterEngine.class);

    /**
     * Path separator in the command line.
     */
    private String pathSeparator;

    /**
     * {@link NutType} of input files.
     */
    private List<NutType> inputNutTypeList;

    /**
     * {@link NutType} of output files.
     */
    private NutType targetNutType;

    /**
     * Command pattern.
     */
    private String command;

    /**
     * The additional libraries.
     * The key is the path and the boolean indicates if the files need to be filtered because they contain tokens.
     */
    private Map<String, Boolean> libraries;

    /**
     * Working directory.
     */
    private File workingDirectory;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param command the command line
     * @param inputNutType the input nut type
     * @param outputNutType the output nut type
     * @param separator the path separator
     * @param convert if this engine is enabled or not
     * @param asynchronous computes version number asynchronously or not
     * @param libs additional libraries paths available in the classpath
     * @throws WuicException if the engine cannot be initialized
     * @throws IOException if any I/O error occurs
     */
    @ConfigConstructor
    @SuppressWarnings("unchecked")
    public CommandLineConverterEngine(@BooleanConfigParam(propertyKey = ApplicationConfig.CONVERT, defaultValue = true) final Boolean convert,
                                      @BooleanConfigParam(propertyKey = ApplicationConfig.COMPUTE_VERSION_ASYNCHRONOUSLY, defaultValue = true) final Boolean asynchronous,
                                      @StringConfigParam(propertyKey = ApplicationConfig.COMMAND, defaultValue = "") final String command,
                                      @StringConfigParam(propertyKey = ApplicationConfig.INPUT_NUT_TYPE, defaultValue = "") final String inputNutType,
                                      @StringConfigParam(propertyKey = ApplicationConfig.OUTPUT_NUT_TYPE, defaultValue = "") final String outputNutType,
                                      @StringConfigParam(propertyKey = ApplicationConfig.PATH_SEPARATOR, defaultValue = " ") final String separator,
                                      @StringConfigParam(propertyKey = ApplicationConfig.LIBRARIES, defaultValue = "") final String libs)
            throws WuicException, IOException {
        super(convert, asynchronous);
        workingDirectory = NutDiskStore.INSTANCE.getWorkingDirectory();

        // Engine won't be associated to any chain
        if (inputNutType.isEmpty() || outputNutType.isEmpty()) {
            inputNutTypeList = Collections.EMPTY_LIST;
            log.info("Properties '{}' and/or '{}' not defined, engine '{}' won't be used",
                    ApplicationConfig.INPUT_NUT_TYPE,
                    ApplicationConfig.OUTPUT_NUT_TYPE,
                    getClass().getSimpleName());
        } else {
            try {
                inputNutTypeList = Arrays.asList(NutType.valueOf(inputNutType));
                targetNutType = NutType.valueOf(outputNutType);
            } catch (IllegalArgumentException iae) {
                WuicException.throwBadArgumentException(new IllegalArgumentException(
                        String.format("Supported NutType are: %s", Arrays.toString(NutType.values()))));
            }

            setCommandAndLibs(command, libs);
            pathSeparator = separator;
        }
    }

    /**
     * <p>
     * Executes the conversion of a given {@code InputStream} represented by the specified {@link ConvertibleNut} thanks
     * to a {@link com.github.wuic.util.TerFunction} that can run the transformation. The function takes as first parameter
     * a list of input files, as second parameter the working directory and as third parameter the result file. The returned
     * {@code Boolean} indicates if the conversion has worked or not.
     * </p>
     *
     * @param is the source input stream
     * @param nut the corresponding nut
     * @param request the request that initiated conversion
     * @throws IOException if any I/O error occurs
     */
    public static InputStream execute(final InputStream is,
                                      final ConvertibleNut nut,
                                      final EngineRequest request,
                                      final BiFunction<List<String>, File, Boolean> executor)
            throws IOException {
        // Do not generate source map if we are in best effort
        final boolean be = request.isBestEffort();
        final List<ConvertibleNut> compositionList;

        if (is instanceof CompositeNut.CompositeInputStream) {
            compositionList = CompositeNut.CompositeInputStream.class.cast(is).getCompositeNut().getCompositionList();
        } else if (nut instanceof CompositeNut) {
            compositionList = CompositeNut.class.cast(nut).getCompositionList();
        } else {
            final String m = "Nut must be a %s or it's InputStream must be an instance of %s";
            throw new IllegalArgumentException(String.format(m, CompositeNut.class.getName(), CompositeNut.CompositeInputStream.class.getName()));
        }

        final List<String> pathsToCompile = new ArrayList<String>(compositionList.size());

        // Read the stream and collect referenced nuts
        for (final ConvertibleNut n : compositionList) {
            if (n.getParentFile() == null) {
                InputStream isNut = null;
                OutputStream osNut = null;

                try {
                    final File file = NutDiskStore.INSTANCE.store(n);

                    if (!file.exists()) {
                        isNut = n.openStream();
                        osNut = new FileOutputStream(file);
                        IOUtils.copyStream(isNut, osNut);
                    }

                    pathsToCompile.add(IOUtils.normalizePathSeparator((file.getAbsolutePath())));
                } catch (InterruptedException ie) {
                    throw new IOException(ie);
                } catch (ExecutionException ee) {
                    throw new IOException(ee);
                } finally {
                    IOUtils.close(isNut, osNut);
                }
            } else {
                pathsToCompile.add(IOUtils.normalizePathSeparator(IOUtils.mergePath(n.getParentFile(), n.getInitialName())));
            }

            if (!be) {
                nut.addReferencedNut(n);
            }
        }

        // Resources to clean
        InputStream sourceMapInputStream = null;
        final File workingDir = NutDiskStore.INSTANCE.getWorkingDirectory();
        final File compilationResult = new File(workingDir, TextAggregatorEngine.aggregationName(NutType.JAVASCRIPT));
        final File sourceMapFile = new File(compilationResult.getAbsolutePath() + ".map");

        final AtomicReference<OutputStream> out = new AtomicReference<OutputStream>();

        OutputStream argsOutputStream = null;

        try {
            log.debug("absolute path: {}", workingDir.getAbsolutePath());

            if (!executor.apply(pathsToCompile, compilationResult)) {
                log.error("executor {} returns false", executor);
                WuicException.throwStreamException(new IOException("Executor failed."));
            } else if (!compilationResult.exists() || (!be && !sourceMapFile.exists())) {
                log.error("{} and/or {} do not exist, which means that some errors break compilation. Check log above to see them.",
                        compilationResult.getAbsolutePath(), sourceMapFile.getAbsolutePath());

                WuicException.throwStreamException(new IOException("Command execution fails, check logs for details."));
            }

            // Read the generated source map
            if (!be) {
                sourceMapInputStream = new FileInputStream(sourceMapFile);
                final String sourceMapName = sourceMapFile.getName();
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                IOUtils.copyStream(sourceMapInputStream, bos);
                final ConvertibleNut sourceMapNut = new ByteArrayNut(bos.toByteArray(), sourceMapName, NutType.MAP, 0L, false);
                nut.setSource(new SourceMapNutImpl(request.getHeap(), nut, sourceMapNut, request.getProcessContext()));
            }

            return new FileInputStream(compilationResult);
        } catch (final WuicException e) {
            throw new IOException(e);
        } finally {
            // Free resources
            IOUtils.close(sourceMapInputStream, out.get(), argsOutputStream);
            IOUtils.delete(compilationResult);
            IOUtils.delete(sourceMapFile);
        }
    }

    /**
     * Executes the given command line and return {@code true} if everything seems to be fine.
     *
     * @param commandLine the command line
     * @param workingDir the working directory
     * @param compilationResult the compilation result location
     * @return {@code true} if command has been executed successfully, {@code false} otherwise
     * @throws IOException if any I/O error occurs
     * @throws InterruptedException if executed process thread is interrupted
     */
    public static Boolean process(final String commandLine, final File workingDir, final File compilationResult)
            throws IOException, InterruptedException {

        // Creates the command line to execute tool
        log.debug("CommandLine arguments: {}", Arrays.asList(commandLine));
        final Process process = new ProcessBuilder(commandLine.split(" "))
                .directory(workingDir)
                .redirectErrorStream(true)
                .start();
        final String errorMessage = IOUtils.readString(new InputStreamReader(process.getInputStream()));

        // Execute tsc tool to generate the source map and javascript file
        // this won't return till 'out' stream being flushed!
        final int exitStatus = process.waitFor();

        if (exitStatus != 0) {
            log.warn("exitStatus: {}", exitStatus);

            if (compilationResult.exists()) {
                log.warn("errorMessage: {}", errorMessage);
            } else {
                log.error("exitStatus: {}", exitStatus);
                log.error("errorMessage: {}", errorMessage);
                return Boolean.FALSE;
            }
        } else if (!errorMessage.isEmpty() && log.isInfoEnabled()) {
            log.info("No error detected, but a message has been generated by command line.");
            log.info(errorMessage);
        }

        return Boolean.TRUE;
    }

    /**
     * <p>
     * Sets the command pattern and additional libraries.
     * The method asserts that mandatory tokens have been defined somewhere.
     * If any token if missing, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param cmd the command pattern
     * @param libs semi colon separated list of libraries to copy to working dir
     * @throws IOException if any I/O error occurs
     */
    private void setCommandAndLibs(final String cmd, final String libs) throws IOException, WuicException{
        AtomicBoolean pathFound = new AtomicBoolean(cmd.contains(PATH_TOKEN));
        AtomicBoolean outPathFound = new AtomicBoolean(cmd.contains(OUT_PATH_TOKEN));
        AtomicBoolean sourceMapFound = new AtomicBoolean(cmd.contains(SOURCE_MAP_TOKEN));
        libraries = new HashMap<String, Boolean>();
        final String[] libArray = libs.split(";");

        // Libraries
        for (final String lib : libArray) {
            if (lib.isEmpty()) {
                continue;
            }

            InputStream is = null;

            try {
                is = getClass().getResourceAsStream(lib);

                // Check if token has to be replaced in the lib
                if (is == null) {
                    libraries.put(lib, Boolean.FALSE);
                } else {
                    addLib(lib, is, pathFound, outPathFound, sourceMapFound);
                }

            } finally {
                IOUtils.close(is);
            }
        }

        // Check path token
        if (!pathFound.get()) {
            WuicException.throwBadArgumentException(
                    new IllegalArgumentException(String.format(ERROR_MESSAGE, cmd,  Arrays.toString(libArray), PATH_TOKEN)));
        }

        // Check out path token
        if (!outPathFound.get()) {
            WuicException.throwBadArgumentException(
                    new IllegalArgumentException(String.format(ERROR_MESSAGE, cmd,  Arrays.toString(libArray), OUT_PATH_TOKEN)));
        }

        // Check source map token
        if (!sourceMapFound.get()) {
            WuicException.throwBadArgumentException(
                    new IllegalArgumentException(String.format(ERROR_MESSAGE, cmd, Arrays.toString(libArray), SOURCE_MAP_TOKEN)));
        }

        // Manage windows platform
        final String osName = System.getProperty("os.name");
        command = (osName != null && osName.contains("Windows") ? "cmd /c " : "") + cmd;
    }

    /**
     * <p>
     * Adds the given library to the map.
     * </p>
     *
     * @param lib the library path
     * @param is the input stream pointing to library content
     * @param pathFound will be set to {@code true} if path token has been found in lib content
     * @param outPathFound will be set to {@code true} if out path token has been found in lib content
     * @param sourceMapFound will be set to {@code true} if source map token has been found in lib content
     */
    private void addLib(final String lib,
                        final InputStream is,
                        final AtomicBoolean pathFound,
                        final AtomicBoolean outPathFound,
                        final AtomicBoolean sourceMapFound) {
        boolean pathFoundInLib = false;
        boolean outPathFoundInLib = false;
        boolean sourceMapFoundInLib = false;
        boolean basePathFoundInLib = false;

        // Go through the scanner
        for (final Scanner sc = new Scanner(is); sc.hasNext(); log.trace(sc.next())) {

            // Check if any token exists
            if (hasNext(PATH_TOKEN, sc)) {
                pathFound.set(true);
                pathFoundInLib = true;
            } else if (hasNext(OUT_PATH_TOKEN, sc)) {
                outPathFound.set(true);
                outPathFoundInLib = true;
            } else if (hasNext(SOURCE_MAP_TOKEN, sc)) {
                sourceMapFound.set(true);
                sourceMapFoundInLib = true;
            } else if (hasNext(BASE_PATH_TOKEN, sc)) {
                basePathFoundInLib = true;
            }

            // All possible tokens found in this file
            if (pathFoundInLib && outPathFoundInLib && sourceMapFoundInLib && basePathFoundInLib) {
                break;
            }
        }

        libraries.put(lib, pathFoundInLib || outPathFoundInLib || sourceMapFoundInLib || basePathFoundInLib);
    }

    /**
     * <p>
     * Method that checks if the next element in a scanner contains to the given token.
     * </p>
     *
     * @param str the token
     * @param scanner the scanner
     * @return {@code true} if next element in the scanner contains the token, {@code false} otherwise
     */
    private boolean hasNext(final String str, final Scanner scanner) {
        return scanner.hasNext(String.format(".*%s.*", Pattern.quote(str)));
    }

    /**
     * <p>
     * Builds the given list of paths in a single {@code String}.
     * </p>
     *
     * @param basePath the common base path, if not {@code null} paths will be relative to it
     * @param pathsToCompile the paths
     * @return the concatenated paths
     */
    private String buildPaths(final List<String> pathsToCompile, final String basePath) {
        final StringBuilder pathsBuilder = new StringBuilder();

        for (final String path : pathsToCompile) {
            if (basePath != null) {
                final String p = path.substring(basePath.length());
                pathsBuilder.append((p.startsWith("/") ? "." : "./") + p).append(pathSeparator);
            } else {
                pathsBuilder.append(path).append(pathSeparator);
            }
        }

        return pathsBuilder.toString();
    }

    /**
     * <p>
     * Installs the declared libraries to the given working directory.
     * </p>
     *
     * @param basePath the base path
     * @param pathsToCompile paths in a list
     * @param paths the paths in a single {@code String}
     * @param outputPath the out put path
     * @param sourceMapPath the source map path
     * @throws IOException if copy fails
     */
    private void installLibraries(final List<String> pathsToCompile,
                                  final String basePath,
                                  final String paths,
                                  final String outputPath,
                                  final String sourceMapPath)
            throws IOException {

        // Install libraries
        for (final Map.Entry<String, Boolean> lib : libraries.entrySet()) {
            InputStream is = null;
            OutputStream os = null;

            try {
                is = getClass().getResourceAsStream(lib.getKey());

                if (is == null) {
                    log.warn("Additional library path {} is not available in the classpath, ignoring...", lib);
                } else {
                    os = new FileOutputStream(new File(workingDirectory, lib.getKey()));

                    // No token to substitute
                    if (!lib.getValue()) {
                        IOUtils.copyStream(is, os);
                    } else {
                        // Replace tokens
                        final StringBuilder content = new StringBuilder(IOUtils.readString(new InputStreamReader(is)));
                        StringUtils.replaceAll(OUT_PATH_TOKEN, outputPath, content);
                        StringUtils.replaceAll(SOURCE_MAP_TOKEN, sourceMapPath, content);

                        // Handle base path and relative paths
                        if (content.indexOf(BASE_PATH_TOKEN) != -1) {
                            if (basePath != null) {
                                StringUtils.replaceAll(BASE_PATH_TOKEN, basePath, content);
                                StringUtils.replaceAll(PATH_TOKEN, paths, content);
                            } else  {
                                final String common = StringUtils.computeCommonPathBeginning(pathsToCompile);
                                StringUtils.replaceAll(BASE_PATH_TOKEN, common, content);
                                StringUtils.replaceAll(PATH_TOKEN, buildPaths(pathsToCompile, common), content);
                            }
                        } else {
                            StringUtils.replaceAll(PATH_TOKEN, paths, content);
                        }

                        // Write replaced content
                        os.write(content.toString().getBytes());
                        os.flush();
                    }
                }
            } finally {
                IOUtils.close(is, os);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NutType targetNutType() {
        return targetNutType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return inputNutTypeList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream transform(final InputStream is, final ConvertibleNut nut, final EngineRequest request)
            throws IOException {
        return execute(is, nut, request, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean apply(final List<String> pathsToCompile, final File compilationResult) {
        try {
            final StringBuilder commandLine = new StringBuilder(command);

            // Handle base path
            final String basePath;

            if (!command.contains(BASE_PATH_TOKEN)) {
                basePath = null;
            } else {
                basePath = StringUtils.computeCommonPathBeginning(pathsToCompile);
                StringUtils.replaceAll(BASE_PATH_TOKEN, basePath, commandLine);
            }

            // Path
            final String paths = buildPaths(pathsToCompile, basePath);
            StringUtils.replaceAll(PATH_TOKEN, paths, commandLine);

            // Source map
            final File sourceMapFile = new File(compilationResult.getAbsolutePath() + ".map");
            final String sourceMapPath = IOUtils.normalizePathSeparator(sourceMapFile.getAbsolutePath());
            StringUtils.replaceAll(SOURCE_MAP_TOKEN, sourceMapPath, commandLine);

            // Output
            final String outPath = IOUtils.normalizePathSeparator(compilationResult.getAbsolutePath());
            StringUtils.replaceAll(OUT_PATH_TOKEN, outPath, commandLine);

            // Install libraries
            installLibraries(pathsToCompile, basePath, paths, outPath, sourceMapPath);

            return process(commandLine.toString(), workingDirectory, compilationResult);
        } catch (IOException ioe) {
            WuicException.throwBadStateException(ioe);
        } catch (InterruptedException ie) {
            WuicException.throwBadStateException(ie);
        }

        return Boolean.TRUE;
    }

    /**
     * <p>
     * Retrieves the working directory where the command line will be executed.
     * </p>
     *
     * @return the working directory
     */
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * <p>
     * Modifies the working directory where the command line will be executed.
     * </p>
     *
     * @param workingDirectory the new working directory
     */
    public void setWorkingDirectory(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
