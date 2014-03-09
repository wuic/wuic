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


package com.github.wuic.plugins.maven.test;

import com.github.wuic.plugins.maven.StaticHelperMojo;
import com.github.wuic.util.IOUtils;
import junit.framework.Assert;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.File;
import java.net.MalformedURLException;

/**
 * <p>
 * Tests for the {@link StaticHelperMojoTest}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.1
 */
@RunWith(JUnit4.class)
public class StaticHelperMojoTest {

    /**
     * <p>
     * Default test.
     * </p>
     *
     * @throws MojoExecutionException if test fails
     * @throws MalformedURLException if test fails
     */
    @Test
    public void defaultTest() throws MojoExecutionException, MalformedURLException {
        final String wuicXml = IOUtils.normalizePathSeparator(getClass().getResource("/wuic.xml").toString());
        final String currentDir = IOUtils.normalizePathSeparator(new File(".").toURI().toURL().toString());
        final String relative =  wuicXml.substring(currentDir.length() - 2);
        System.out.println(wuicXml);
        System.out.println(currentDir);
        System.out.println(relative);

        // Create MOJO
        final StaticHelperMojo mojo = new StaticHelperMojo();
        mojo.setRelocateTransformedXml(Boolean.TRUE);
        mojo.setXml(relative);
        mojo.setOutput("generated");

        // Mock
        final MavenProject mavenProject = Mockito.mock(MavenProject.class);
        final Build build = Mockito.mock(Build.class);
        Mockito.when(build.getDirectory()).thenReturn(new File(System.getProperty("java.io.tmpdir"), "wuic-static-test").getAbsolutePath());
        Mockito.when(build.getOutputDirectory()).thenReturn("");
        Mockito.when(mavenProject.getBuild()).thenReturn(build);
        Mockito.when(mavenProject.getBasedir()).thenReturn(new File("."));
        mojo.setMavenProject(mavenProject);
        final MavenProjectHelper helper = Mockito.mock(MavenProjectHelper.class);
        mojo.setProjectHelper(helper);

        // Invoke
        mojo.execute();

        // Verify
        final File parent = new File(System.getProperty("java.io.tmpdir"), "wuic-static-test/generated/");
        Assert.assertTrue(new File(parent,"css").listFiles()[0].list()[0].equals("aggregate.css"));
        Assert.assertTrue(new File(parent, "js").listFiles()[0].list()[0].equals("aggregate.js"));
    }
}