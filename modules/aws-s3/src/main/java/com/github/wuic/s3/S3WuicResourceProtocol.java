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


package com.github.wuic.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.github.wuic.FileType;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.resource.WuicResourceProtocol;
import com.github.wuic.resource.impl.InputStreamWuicResource;
import com.github.wuic.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.resource.WuicResourceProtocol} implementation for S3 AWS Cloud accesses.
 * </p>
 *
 * @author Corentin AZELART
 * @version 1.0
 * @since 0.3.3
 */
public class S3WuicResourceProtocol implements WuicResourceProtocol {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The client connected to the S3 AWS.
     */
    private AmazonS3Client amazonS3Client;

    /**
     * Bucket name.
     */
    private String bucketName;

    /**
     * The base path where to move.
     */
    private String basePath;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param bucket the bucket name
     * @param accessKey the user access key
     * @param secretKey the user private key
     * @param path the root path
     */
    public S3WuicResourceProtocol(final String bucket, final String accessKey, final String secretKey, final String path) {
        bucketName = bucket;
        basePath = path;

        if (accessKey != null && secretKey != null) {
            amazonS3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listResourcesPaths(final Pattern pattern) throws IOException {
        return recursiveSearch(basePath, pattern);
    }

    /**
     * <p>
     * Searches recursively in the given path any files matching the given entry.
     * </p>
     *
     * @param path the path
     * @param pattern the pattern to match
     * @return the list of matching files
     * @throws java.io.IOException if the client can't move to a directory or any I/O error occurs
     */
    private List<String> recursiveSearch(final String path, final Pattern pattern) throws IOException {

        ObjectListing objectListing = null;
        try {
            final String finalSuffix =  path.equals("") ? "" : "/";
            objectListing = amazonS3Client.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(path + finalSuffix).withDelimiter("/"));
        } catch (AmazonServiceException ase) {
            final StringBuilder aseMessageBuilder = new StringBuilder("Can't get S3Object on bucket ")
                    .append(bucketName).append(" for resource key : ").append(path);
            throw new IOException(aseMessageBuilder.toString(), ase);
        }

        final List<String> retval = new ArrayList<String>();
        for (final S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
            // Ignore directories, all resources are in the listing
            if (!s3ObjectSummary.getKey().endsWith("/")) {
                final Matcher matcher = pattern.matcher(s3ObjectSummary.getKey());

                if (matcher.find()) {
                    retval.add(s3ObjectSummary.getKey());
                }
            }
        }

        // Recursive search on prefixes (directories)
        for (final String s3CommonPrefix : objectListing.getCommonPrefixes()) {
            retval.addAll(recursiveSearch(s3CommonPrefix.substring(0, s3CommonPrefix.length() - 1), pattern));
        }

        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResource accessFor(final String realPath, final FileType type) throws IOException {
        // Try to get S3 object
        S3Object s3Object = null;
        try {
            s3Object = amazonS3Client.getObject(bucketName, realPath);
        } catch (AmazonServiceException ase) {
            final StringBuilder aseMessageBuilder = new StringBuilder("Can't get S3Object on bucket ")
                    .append(bucketName).append(" for resource key : ").append(realPath);
            throw new IOException(aseMessageBuilder.toString(), ase);
        }

        S3ObjectInputStream s3ObjectInputStream = null;
        try {
            // Get S3Object content
            s3ObjectInputStream = s3Object.getObjectContent();

            // Download file into memory
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(IOUtils.WUIC_BUFFER_LEN);
            IOUtils.copyStream(s3ObjectInputStream, baos);

            // Create resource
            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return new InputStreamWuicResource(bais, realPath, type);
        } finally {
            // Close S3Object stream
            if(s3ObjectInputStream != null) {
                s3ObjectInputStream.close();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            log.debug("Disconnecting from S3 AWS Cloud...");

            // This object if not referenced and is going to be garbage collected.
            // Do not keep the client connected.
            amazonS3Client.shutdown();
        } finally {
            super.finalize();
        }
    }
}
