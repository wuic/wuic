/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
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


package com.github.wuic.plugins.maven;

import com.github.wuic.WuicFacade;
import com.github.wuic.engine.core.StaticEngineBuilder;
import com.github.wuic.engine.impl.embedded.StaticEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;
import com.github.wuic.xml.XmlBuilderBean;
import com.github.wuic.xml.XmlWorkflowBean;
import com.github.wuic.xml.XmlWorkflowTemplateBean;
import com.github.wuic.xml.XmlWuicBean;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This MOJO helps to process nuts for static usage. "Static" means that nuts can't be processed at runtime so we have
 * to invoke engine's chain of responsibility at build-time. This plugin supports only configuration from XML file.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.1
 * @since 0.4.1
 */
@Mojo(name = "process", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class StaticHelperMojo extends AbstractMojo {

    /**
     * Fail message to display when an error occurs.
     */
    private static final String FAIL_MESSAGE = String.format("Unable to run %s", StaticHelperMojo.class.getName());

    /**
     * The logger.
     */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Maven project.
     */
    @Component
    private MavenProject project;

    /**
     * <p>
     * Used to make addition of resources simpler.
     * </p>
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The "xml" configuration parameter.
     */
    @Parameter(required = true)
    private String xml;

    /**
     * Includes the transformed XML file as source file named 'wuic.xml'.
     */
    @Parameter(defaultValue = "false")
    private Boolean relocateTransformedXml;

    /**
     * Directory where process result should be written.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private String output;

    /**
     * Base path where every processed statics referenced by HTML will be served.
     */
    @Parameter(defaultValue = "/")
    private String contextPath;

    /**
     * <p>
     * Adds into the classpath the project's resources.
     * </p>
     *
     * @throws MojoExecutionException if an error occurs
     * @throws IOException if an I/O error occurs
     */
    @SuppressWarnings("unchecked")
    private void setClasspath() throws MojoExecutionException, IOException {
        final URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();

        try {
            for (final Resource artifact : (List<Resource>) project.getResources()) {
                getLog().info(artifact.getDirectory());
                final Class urlClass = URLClassLoader.class;
                final Method method = urlClass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(urlClassLoader, new File(artifact.getDirectory()).toURI().toURL());
            }
        } catch (NoSuchMethodException nsme) {
            throw new MojoExecutionException(FAIL_MESSAGE, nsme);
        } catch (IllegalAccessException iae) {
            throw new MojoExecutionException(FAIL_MESSAGE, iae);
        } catch (InvocationTargetException ite) {
            throw new MojoExecutionException(FAIL_MESSAGE, ite);
        }
    }

    /**
     * <p>
     * Writes the given XML to transform into the disk.
     * </p>
     *
     * @param xmlFile the XML file
     * @throws JAXBException if JAXB fails
     * @throws IOException if an I/O error occurs
     */
    private void relocateTransformedXml(final File xmlFile) throws JAXBException, IOException {

        // Now load XML configuration to update it in order to use chains of responsibility with static engine
        final JAXBContext jc = JAXBContext.newInstance(XmlWuicBean.class);
        final Unmarshaller unmarshaller = jc.createUnmarshaller();
        final XmlWuicBean bean = (XmlWuicBean) unmarshaller.unmarshal(xmlFile);

        // Override workflow definition
        final String staticEngineName = "wuic" + StaticEngineBuilder.class.getSimpleName();
        final String workflowTemplateId = staticEngineName + "Template";
        final XmlWorkflowTemplateBean template = new XmlWorkflowTemplateBean();
        template.setEngineBuilderIds(Arrays.asList(staticEngineName));
        template.setUseDefaultEngines(Boolean.FALSE);
        template.setId(workflowTemplateId);

        if (bean.getWorkflowTemplates() != null) {
            bean.getWorkflowTemplates().add(template);
        } else {
            bean.setWorkflowTemplates(Arrays.asList(template));
        }

        final XmlWorkflowBean workflow = new XmlWorkflowBean();
        workflow.setHeapIdPattern(".*");
        workflow.setWorkflowTemplateId(workflowTemplateId);
        workflow.setIdPrefix("");

        if (bean.getWorkflows() != null) {
            bean.getWorkflows().add(workflow);
        } else {
            bean.setWorkflows(Arrays.asList(workflow));
        }

        // Declare the static engine
        final XmlBuilderBean builder = new XmlBuilderBean();
        builder.setId(staticEngineName);
        builder.setType(StaticEngineBuilder.class.getSimpleName());

        bean.setEngineBuilders(Arrays.asList(builder));

        // Write modifies configuration into the disk
        final File temp = File.createTempFile("tempXml", Long.toString(System.nanoTime()));
        final File outputXmlFile = new File(temp, "wuic.xml");

        if (!temp.delete() || !temp.mkdirs()) {
            throw new IOException(String.format("Could not delete temp '%s' directory for transformed XML configuration file: '%s'",
                    temp.getAbsolutePath(), "wuic.xml"));
        }

        final Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
        marshaller.marshal(bean, outputXmlFile);

        // Include the written file as a resource
        final List<String> includes = Arrays.asList("wuic.xml");
        projectHelper.addResource(project, temp.getAbsolutePath(), includes, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException {
        try {
            setClasspath();

            // Load wuic.xml file and create facade
            final File xmlFile = new File(project.getBasedir(), xml);
            final WuicFacade facade = WuicFacade.newInstance(contextPath, xmlFile.toURI().toURL(), Boolean.TRUE);

            // Now write each workflow result to disk with its description file
            for (final String wId : facade.workflowIds()) {
                PrintWriter pw = null;

                try {
                    final File file = new File(project.getBuild().getOutputDirectory(), String.format(StaticEngine.STATIC_WORKFLOW_FILE, wId));

                    if (!file.getParentFile().mkdirs()) {
                        log.error("Unable to create '{}' directory", file.getParent());
                    }

                    pw = new PrintWriter(file);
                    final List<Nut> nuts = facade.runWorkflow(wId);

                    for (final Nut nut : nuts) {
                        write(nut, wId, pw);
                    }
                } finally {
                    IOUtils.close(pw);
                }
            }

            // No need to continue if we don't want to generate wuic.xml file too
            if (relocateTransformedXml) {
                relocateTransformedXml(xmlFile);
            }
        } catch (WuicException we) {
            throw new MojoExecutionException(FAIL_MESSAGE, we);
        } catch (MalformedURLException mue) {
            throw new MojoExecutionException(FAIL_MESSAGE, mue);
        } catch (JAXBException je) {
            throw new MojoExecutionException(FAIL_MESSAGE, je);
        } catch (IOException ioe) {
            throw new MojoExecutionException(FAIL_MESSAGE, ioe);
        }
    }

    /**
     * <p>
     * Writes the given net into the output directory.
     * </p>
     *
     * @param nut the nut to be written
     * @param wId the workflow ID
     * @param workflowWriter the workflow print writer
     * @throws WuicException if WUIC fails
     * @throws FileNotFoundException if output directory can't be reached
     */
    public void write(final Nut nut, final String wId, final PrintWriter workflowWriter) throws WuicException, FileNotFoundException {
        final String path = nut.getProxyUri() == null ? IOUtils.mergePath(nut.getVersionNumber().toString(), nut.getName()) : nut.getProxyUri();
        workflowWriter.println(String.format("%s %s", path, nut.getNutType().getExtensions()[0]));
        final File file = new File(project.getBuild().getOutputDirectory().equals(output) ?
                output : IOUtils.mergePath(project.getBuild().getDirectory(), output),  IOUtils.mergePath(wId, nut.getVersionNumber().toString(), nut.getName()));

        // Create if not exist
        if (file.getParentFile() != null && file.getParentFile().mkdirs()) {
            log.debug("{} created", file.getParent());
        }

        IOUtils.copyStream(nut.openStream(), new FileOutputStream(file));

        // Recursive call on referenced nuts
        if (nut.getReferencedNuts() != null) {
            for (final Nut ref : nut.getReferencedNuts()) {
                write(ref, wId, workflowWriter);
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
     * Sets the relocate transformed XML flag.
     * </p>
     *
     * @param relocateTransformedXml the new flag
     */
    public void setRelocateTransformedXml(final Boolean relocateTransformedXml) {
        this.relocateTransformedXml = relocateTransformedXml;
    }

    /**
     * <p>
     * Sets the maven project.
     * </p>
     *
     * @param mp the maven project
     */
    public void setMavenProject(final MavenProject mp) {
        project = mp;
    }

    /**
     * <p>
     * Sets the helper.
     * </p>
     *
     * @param projectHelper the helper
     */
    public void setProjectHelper(final MavenProjectHelper projectHelper) {
        this.projectHelper = projectHelper;
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
}