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


package com.github.wuic;

import com.github.wuic.config.bean.BuilderBean;
import com.github.wuic.config.bean.HeapBean;
import com.github.wuic.config.bean.WorkflowBean;
import com.github.wuic.config.bean.WorkflowTemplateBean;
import com.github.wuic.config.bean.WuicBean;
import com.github.wuic.context.ContextBuilder;
import com.github.wuic.engine.core.StaticEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.ConvertibleNut;
import com.github.wuic.nut.dao.core.UnreachableNutDao;
import com.github.wuic.util.IOUtils;
import com.github.wuic.util.NutUtils;
import com.github.wuic.util.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>
 * This class executes WUIC as a task for build time processing. It basically takes 'wuic.xml' path and run all
 * configured workflow. All results can be copied to a specific directory in order to bundle them in a package.
 * </p>
 *
 * <p>
 * This class can be integrated with ant as a custom task since it respects its name conventions.
 * </p>
 *
 * @author Guillaume DROUET
 * @since 0.5.2
 */
public class WuicTask {

    /**
     * Information file generated at build time that help to configure the servlet container at runtime.
     */
    public static final String BUILD_INFO_FILE = "wuic-build-info.properties";

    /**
     * XML file name.
     */
    private static final String XML_FILE = "wuic.xml";

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The "xml" configuration parameter.
     */
    private String xml;

    /**
     * Where the transformed XML file should be relocated as a source file named 'wuic.xml'.
     */
    private String relocateTransformedXmlTo;

    /**
     * Directory where process result should be written.
     */
    private String output;

    /**
     * Base path where every processed statics referenced by HTML will be served.
     */
    private String contextPath;

    /**
     * The 'wuic.properties' file location (optional).
     */
    private String properties;

    /**
     * Charset to use when writing to the disk.
     */
    private String charset;

    /**
     * The profiles to enable.
     */
    private String profiles;

    /**
     * Pattern matching file names to moved to the top directory.
     */
    private Pattern moveToTopDirPattern;

    /**
     * <p>
     * Builds a new instance. {@link #relocateTransformedXml} is {@code null} and default {@link #contextPath} is '/'.
     * </p>
     */
    public WuicTask() {
        this.relocateTransformedXmlTo = null;
        this.contextPath = "/";
        this.charset = "UTF-8";
    }

    /**
     * <p>
     * Writes the given XML to transform into the disk.
     * </p>
     *
     * @param xmlFile the XML file
     * @return the relocated XML file path
     * @throws javax.xml.bind.JAXBException if JAXB fails
     * @throws IOException if an I/O error occurs
     */
    private String relocateTransformedXml(final File xmlFile) throws JAXBException, IOException {

        // Now load XML configuration to update it in order to use chains of responsibility with static engine
        final JAXBContext jc = JAXBContext.newInstance(WuicBean.class);
        final Unmarshaller unmarshaller = jc.createUnmarshaller();
        final WuicBean bean = (WuicBean) unmarshaller.unmarshal(xmlFile);

        // Set mock DAO for heaps
        if (bean.getHeaps() != null) {
            final String id = ContextBuilder.getDefaultBuilderId(UnreachableNutDao.class);
            final BuilderBean dao = new BuilderBean();
            dao.setId(id);
            dao.setType(UnreachableNutDao.class.getSimpleName());

            if (bean.getDaoBuilders() != null) {
                bean.getDaoBuilders().add(dao);
            } else {
                bean.setDaoBuilders(Arrays.asList(dao));
            }

            for (final HeapBean heap : bean.getHeaps()) {
                heap.setBuilderId(id);
            }
        }

        // Override workflow definition
        final String staticEngineName = "wuic" + StaticEngine.class.getSimpleName() + "Builder";
        final String workflowTemplateId = staticEngineName + "Template";
        final WorkflowTemplateBean template = new WorkflowTemplateBean();
        template.setEngineBuilderIds(Arrays.asList(staticEngineName));
        template.setUseDefaultEngines(Boolean.FALSE);
        template.setId(workflowTemplateId);

        if (bean.getWorkflowTemplates() != null) {
            bean.getWorkflowTemplates().add(template);
        } else {
            bean.setWorkflowTemplates(Arrays.asList(template));
        }

        final WorkflowBean workflow = new WorkflowBean();
        workflow.setHeapIdPattern(".*");
        workflow.setWorkflowTemplateId(workflowTemplateId);
        workflow.setIdPrefix("");

        if (bean.getWorkflows() != null) {
            bean.getWorkflows().add(workflow);
        } else {
            bean.setWorkflows(Arrays.asList(workflow));
        }

        // Declare the static engine
        final BuilderBean builder = new BuilderBean();
        builder.setId(staticEngineName);
        builder.setType(StaticEngine.class.getSimpleName() + "Builder");

        bean.setEngineBuilders(Arrays.asList(builder));

        // Write modifies configuration into the disk
        final File outputXmlFile = new File(relocateTransformedXmlTo, XML_FILE);

        final Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
        marshaller.marshal(bean, outputXmlFile);

        return outputXmlFile.getName();
    }

    /**
     * <p>
     * Indicates if the name matches the file to be located on top of output directory.
     * A special case is supported for files ending with {@code .appcache}: if the name without that prefix matches the
     * {@link #moveToTopDirPattern}, then it's also moved.
     * </p>
     *
     * @param name the name
     * @return {@code true} if pattern matches, {@code false} otherwise
     */
    private boolean locateOnTop(final String name) {
        if (moveToTopDirPattern == null) {
            return false;
        }

        final int appCacheIndex = name.indexOf(".appcache");
        return moveToTopDirPattern.matcher(appCacheIndex != -1 ? name.substring(0, appCacheIndex) : name).matches();
    }

    /**
     * <p>
     * Executes the task. The method is named with ANT task naming conventions to be integrated with ANT.
     * </p>
     *
     * @throws WuicException if WUIC fails to process nuts or configure
     * @throws JAXBException if wuic.xml can't be read
     * @throws IOException   if any I/O error occurs
     */
    public void execute() throws WuicException, JAXBException, IOException {
       executeTask();
    }

    /**
     * <p>
     * Executes the process by reading the 'wuic.xml', executing all discovered workflow and writing result to the output
     * location.
     * </p>
     *
     * @return the list of files with result metadata relocated with the {@link #relocateTransformedXmlTo} location
     * @throws WuicException if WUIC fails to process nuts or configure
     * @throws JAXBException if wuic.xml can't be read
     * @throws IOException   if any I/O error occurs
     */
    public List<String> executeTask() throws WuicException, JAXBException, IOException {
        if (xml == null) {
            WuicException.throwBadArgumentException(new IllegalArgumentException("wuic.xml location can't be null."));
        }

        if (output == null) {
            WuicException.throwBadArgumentException(new IllegalArgumentException("Output location can't be null."));
        }

        final List<String> retval = new ArrayList<String>();

        // Load wuic.xml file and create facade
        final File xmlFile = new File(xml);
        final WuicFacadeBuilder facadeBuilder = new WuicFacadeBuilder()
                .contextPath(contextPath)
                .wuicConfigurationPath(xmlFile.toURI().toURL());

        if (profiles != null) {
            facadeBuilder.contextBuilder().enableProfile(profiles.split(",")).toFacade();
        }

        final WuicFacade facade;

        if (properties == null) {
            facade = facadeBuilder.build();
        } else {
            final File propertyFile = new File(properties);
            facade = facadeBuilder.wuicPropertiesPath(propertyFile.toURI().toURL()).build();
        }

        PrintWriter buildInfo = null;
        final File buildInfoFile = new File(relocateTransformedXmlTo, BUILD_INFO_FILE);

        try {
            // Going to embed the result in order to server it from the webapp, add information for runtime initialization
            if (relocateTransformedXmlTo != null) {
                if (!buildInfoFile.getParentFile().mkdirs()) {
                    log.error("Unable to create '{}' directory", buildInfoFile.getParent());
                }

                buildInfo = new PrintWriter(buildInfoFile);
                buildInfo.write(ApplicationConfig.WUIC_SERVLET_CONTEXT_PARAM + '=' + contextPath);
                buildInfo.write(IOUtils.NEW_LINE);
                buildInfo.write("workflowList=");
            }

            // Now write each workflow result to disk with its description file
            int cpt = 0;
            int len = facade.workflowIds().size();

            for (final String wId : facade.workflowIds()) {
                PrintWriter pw = null;

                try {
                    if  (relocateTransformedXmlTo != null) {
                        final String fileName = String.format(StaticEngine.STATIC_WORKFLOW_FILE, wId);
                        final File file = new File(relocateTransformedXmlTo, fileName);

                        if (!file.getParentFile().mkdirs()) {
                            log.error("Unable to create '{}' directory", file.getParent());
                        }

                        // Adds file name without '/' at the beginning
                        retval.add(fileName.substring(1));

                        pw = new PrintWriter(file);
                        buildInfo.write(wId);

                        if (cpt++ < len - 1) {
                            buildInfo.write('\t');
                        }
                    }

                    final List<ConvertibleNut> nuts = facade.runWorkflow(wId, ProcessContext.DEFAULT);

                    for (final ConvertibleNut nut : nuts) {
                        write(nut, wId, pw, 0);
                    }
                } finally {
                    IOUtils.close(pw);
                }
            }
        } finally {
            IOUtils.close(buildInfo);
        }

        // No need to continue if we don't want to generate wuic.xml file too
        if (relocateTransformedXmlTo != null) {
            retval.add(relocateTransformedXml(xmlFile));
            retval.add(buildInfoFile.getName());
        }

        return retval;
    }

    /**
     * <p>
     * Writes the given net into the output directory.
     * </p>
     *
     * @param nut   the nut to be written
     * @param wId   the workflow ID
     * @param depth the depth computed from referenced nuts chain
     * @throws WuicException if WUIC fails
     * @throws IOException   if output directory can't be reached or if transformation fails
     */
    public void write(final ConvertibleNut nut, final String wId, final PrintWriter workflowWriter, final int depth)
            throws WuicException, IOException {
        final String path = nut.getProxyUri() == null ? IOUtils.mergePath(String.valueOf(NutUtils.getVersionNumber(nut)), nut.getName()) : nut.getProxyUri();

        if (workflowWriter != null) {
            for (int i = 0; i < depth; i++) {
                workflowWriter.print('\t');
            }

            workflowWriter.println(String.format("%s %s", path, nut.getInitialNutType().getExtensions()[0]));
        }

        final File file;

        // Keep the file in the top directory
        if (locateOnTop(nut.getInitialName())) {
            file = new File(output, nut.getName());
        } else {
            file = new File(output, IOUtils.mergePath(wId, String.valueOf(NutUtils.getVersionNumber(nut)), nut.getName()));
        }

        // Create if not exist
        if (file.getParentFile() != null && file.getParentFile().mkdirs()) {
            log.debug("{} created", file.getParent());
        }

        final OutputStream os = new FileOutputStream(file);

        if (!nut.getNutType().isText()) {
            nut.transform(new Pipe.DefaultOnReady(os));
        } else {
            nut.transform(new Pipe.DefaultOnReady(os, charset));
        }

        // Recursive call on referenced nuts
        if (nut.getReferencedNuts() != null) {
            for (final ConvertibleNut ref : nut.getReferencedNuts()) {
                write(ref, wId, workflowWriter, depth + 1);
            }
        }
    }

    /**
     * <p>
     * Sets the "xml" configuration parameter.
     * </p>
     *
     * @param x the parameter
     */
    public void setXml(final String x) {
        xml = x;
    }

    /**
     * <p>
     * Sets the "properties" configuration parameter.
     * </p>
     *
     * @param properties the parameter
     */
    public void setProperties(final String properties) {
        this.properties = properties;
    }

    /**
     * <p>
     * Sets the relocate location for transformed XML.
     * </p>
     *
     * @param relocateTransformedXml the new location
     */
    public void setRelocateTransformedXmlTo(final String relocateTransformedXml) {
        this.relocateTransformedXmlTo = relocateTransformedXml;
    }

    /**
     * <p>
     * Sets the output directory.
     * </p>
     *
     * @param output the output directory
     */
    public void setOutput(final String output) {
        this.output = output;
    }

    /**
     * <p>
     * Sets the context path.
     * </p>
     *
     * @param contextPath the context path
     */
    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * Charset to use when writing to the disk.
     */
    public void setCharset(final String charset) {
        this.charset = charset;
    }

    /**
     * <p>
     * Sets the profiles.
     * </p>
     *
     * @param profiles the profiles
     */
    public void setProfiles(final String profiles) {
        this.profiles = profiles;
    }

    /**
     * <p>
     * Sets the pattern matching name of files to be moved to the top.
     * </p>
     *
     * @param moveToTopDirPattern the pattern
     */
    public void setMoveToTopDirPattern(final String moveToTopDirPattern) {
        if (moveToTopDirPattern != null) {
            this.moveToTopDirPattern = Pattern.compile(moveToTopDirPattern);
        }
    }
}
