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
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutDiskStore;
import com.github.wuic.util.StringUtils;
import com.github.wuic.util.TerFunction;
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

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
        implements TerFunction<List<String>, File, File, Boolean> {

    /**
     * The base path token in the command line pattern.
     */
    public static final String BASE_PATH_TOKEN = "%basePaths%";

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
    public static final String ERROR_MESSAGE = "Command '%s' must contains the token '%s'";

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
     * @throws com.github.wuic.exception.WuicException if the engine cannot be initialized
     */
    @ConfigConstructor
    public CommandLineConverterEngine(@BooleanConfigParam(propertyKey = ApplicationConfig.CONVERT, defaultValue = true) final Boolean convert,
                                      @BooleanConfigParam(propertyKey = ApplicationConfig.COMPUTE_VERSION_ASYNCHRONOUSLY, defaultValue = true) final Boolean asynchronous,
                                      @StringConfigParam(propertyKey = ApplicationConfig.COMMAND, defaultValue = "") final String command,
                                      @StringConfigParam(propertyKey = ApplicationConfig.INPUT_NUT_TYPE, defaultValue = "") final String inputNutType,
                                      @StringConfigParam(propertyKey = ApplicationConfig.OUTPUT_NUT_TYPE, defaultValue = "") final String outputNutType,
                                      @StringConfigParam(propertyKey = ApplicationConfig.PATH_SEPARATOR, defaultValue = " ") final String separator)
            throws WuicException {
        super(convert, asynchronous);

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

            setCommand(command);
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
                                      final TerFunction<List<String>, File, File, Boolean> executor)
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

                    pathsToCompile.add(file.getAbsolutePath());
                } catch (InterruptedException ie) {
                    throw new IOException(ie);
                } catch (ExecutionException ee) {
                    throw new IOException(ee);
                } finally {
                    IOUtils.close(isNut, osNut);
                }
            } else {
                pathsToCompile.add(IOUtils.mergePath(n.getParentFile(), n.getName()));
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

            if (!executor.apply(pathsToCompile, workingDir, compilationResult)) {
                log.error("executor {} returns false", executor);
                WuicException.throwStreamException(new IOException("Executor failed."));
            } else if (!compilationResult.exists()) {
                log.error("{} does not exists, which means that some errors break compilation. Check log above to see them.");
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
        final Process process = new ProcessBuilder(commandLine.toString().split(" "))
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
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

    /**
     * <p>
     * Sets the command pattern and asserts that mandatory tokens have been defined.
     * If any token if missing, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param cmd the command pattern
     */
    private void setCommand(final String cmd) {

        // Check path token
        if (!cmd.contains(PATH_TOKEN)) {
            WuicException.throwBadArgumentException(new Exception(String.format(ERROR_MESSAGE, cmd, PATH_TOKEN)));
        }

        // Check source map token
        if (!cmd.contains(SOURCE_MAP_TOKEN)) {
            WuicException.throwBadArgumentException(new Exception(String.format(ERROR_MESSAGE, cmd, SOURCE_MAP_TOKEN)));
        }

        // Check out path token
        if (!cmd.contains(OUT_PATH_TOKEN)) {
            WuicException.throwBadArgumentException(new Exception(String.format(ERROR_MESSAGE, cmd, OUT_PATH_TOKEN)));
        }

        // Manage windows platform
        final String osName = System.getProperty("os.name");
        command = (osName != null && osName.contains("Windows") ? "cmd /c " : "") + cmd;
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
    public Boolean apply(final List<String> pathsToCompile, final File workingDir, final File compilationResult) {
        try {
            final StringBuilder commandLine = new StringBuilder(command);

            // Handle base path
            final int basePathIndex = command.indexOf(BASE_PATH_TOKEN);

            final String basePath;

            if (basePathIndex == -1) {
                basePath = "";
            } else {
                basePath = StringUtils.computeCommonPathBeginning(pathsToCompile);
                commandLine.replace(basePathIndex, BASE_PATH_TOKEN.length(), basePath);
            }

            // Path
            int pathIndex = -1;

            // Replace each occurrence
            while ((pathIndex = command.indexOf(PATH_TOKEN, pathIndex + 1)) != -1) {
                commandLine.replace(pathIndex, pathIndex + PATH_TOKEN.length(), "");

                for (int i = pathsToCompile.size() - 1; i >= 0; i--) {
                    // Remove base path
                    commandLine.insert(pathIndex, pathsToCompile.get(i).substring(basePath.length()));

                    if (i != 0) {
                        commandLine.insert(pathIndex, pathSeparator);
                    }
                }
            }

            // Source map
            final File sourceMapFile = new File(compilationResult.getAbsolutePath() + ".map");
            int sourceMapIndex = -1;

            // Replace each occurrence
            while ((sourceMapIndex = commandLine.indexOf(SOURCE_MAP_TOKEN, sourceMapIndex + 1)) != -1) {
                commandLine.replace(sourceMapIndex, sourceMapIndex + SOURCE_MAP_TOKEN.length(), sourceMapFile.getAbsolutePath());
            }

            // output
            int outIndex = -1;

            // Replace each occurrence
            while ((outIndex = commandLine.indexOf(OUT_PATH_TOKEN, outIndex + 1)) != -1) {
                commandLine.replace(outIndex, outIndex + OUT_PATH_TOKEN.length(), compilationResult.getAbsolutePath());
            }

            return process(commandLine.toString(), workingDir, compilationResult);
        } catch (IOException ioe) {
            WuicException.throwBadStateException(ioe);
        } catch (InterruptedException ie) {
            WuicException.throwBadStateException(ie);
        }

        return Boolean.TRUE;
    }
}
