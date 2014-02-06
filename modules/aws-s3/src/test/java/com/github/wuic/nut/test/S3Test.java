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


package com.github.wuic.nut.test;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.github.wuic.ApplicationConfig;
import com.github.wuic.NutType;
import com.github.wuic.engine.impl.embedded.CGTextAggregatorEngine;
import com.github.wuic.engine.impl.yuicompressor.CssYuiCompressorEngine;
import com.github.wuic.nut.NutsHeap;
import com.github.wuic.engine.Engine;
import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.impl.ehcache.EhCacheEngine;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.s3.S3NutDao;
import com.github.wuic.nut.s3.S3NutDaoBuilder;
import com.github.wuic.util.IOUtils;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * <p>
 * Tests for S3 AWS module.
 * </p>
 *
 * @author Corentin AZELART
 * @version 1.3
 * @since 0.3.3
 */
@RunWith(JUnit4.class)
public class S3Test {

    /**
     * <p>
     * Test builder.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void builderTest() throws Exception {
        new S3NutDaoBuilder()
                .property(ApplicationConfig.CLOUD_BUCKET, "bucket")
                .property(ApplicationConfig.LOGIN, "login")
                .property(ApplicationConfig.PASSWORD, "password")
                .build();

        try {
            new S3NutDaoBuilder().property("foo", "value");
            Assert.fail();
        } catch (Exception e) {
            // Normal behavior : property not supported
        }
    }

    /**
     * <p>
     * Tests the S3 access.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void s3Test() throws Exception {
        // Create a real object and mock its initClient method
        final S3NutDao dao = spy(new S3NutDao("/path", false, null, -1, "wuic", "login", "pwd", false));

        // Build client mock
        final AmazonS3Client client = mock(AmazonS3Client.class);
        final ObjectMetadata metadata = mock(ObjectMetadata.class);
        when(metadata.getLastModified()).thenReturn(new Date());
        when(client.getObjectMetadata(anyString(), anyString())).thenReturn(metadata);
        when(dao.initClient()).thenReturn(client);

        // List returned by client
        final ObjectListing list = mock(ObjectListing.class);
        final S3ObjectSummary summary = mock(S3ObjectSummary.class);
        when(summary.getKey()).thenReturn("[cloud].css");
        final S3ObjectSummary summarBis = mock(S3ObjectSummary.class);
        when(summarBis.getKey()).thenReturn("cloud.css");
        when(client.listObjects(any(ListObjectsRequest.class))).thenReturn(list);
        when(list.getObjectSummaries()).thenReturn(Arrays.asList(summary, summarBis));

        // Bytes returned by mocked S3
        final byte[] array = ".cloud { text-align : justify;}".getBytes();
        final S3Object object = mock(S3Object.class);
        when(object.getObjectContent()).thenReturn(new S3ObjectInputStream(new ByteArrayInputStream(array), null));
        when(client.getObject(anyString(), anyString())).thenReturn(object);

        // TODO : problem here : we specify '[cloud.css]' but getNuts() returns 'cloud.css' because regex are always activated !
        final NutsHeap nutsHeap = new NutsHeap(Arrays.asList("[cloud].css"), dao, "heap");
        Assert.assertEquals(nutsHeap.getNuts().size(), 1);

        final Engine compressor = new CssYuiCompressorEngine(true, "UTF-8", -1);
        final Engine cacheEngine = new EhCacheEngine(false, null);
        final Engine aggregator = new CGTextAggregatorEngine(true);
        cacheEngine.setNext(compressor);
        compressor.setNext(aggregator);

        final List<Nut> group = cacheEngine.parse(new EngineRequest("", "", nutsHeap, new HashMap<NutType, Engine>()));

        Assert.assertFalse(group.isEmpty());
        InputStream is;

        for (Nut res : group) {
            is = res.openStream();
            Assert.assertTrue(IOUtils.readString(new InputStreamReader(is)).length() > 0);
            is.close();
        }
    }
}
