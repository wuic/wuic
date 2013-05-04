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


package com.github.wuic.ftp;

import com.github.wuic.FileType;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.resource.WuicResourceProtocol;
import com.github.wuic.resource.impl.InputStreamWuicResource;
import com.github.wuic.util.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.resource.WuicResourceProtocol} implementation for FTP accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.1
 */
public class FtpWuicResourceProtocol implements WuicResourceProtocol {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The client connected to the server.
     */
    private FTPClient ftpClient;

    /**
     * The host name.
     */
    private String hostName;

    /**
     * The base path where to move.
     */
    private String basePath;

    /**
     * The user name.
     */
    private String userName;

    /**
     * The password.
     */
    private String password;

    /**
     * The port.
     */
    private int port;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param ftps use FTPS or FTP protocol
     * @param host the host name
     * @param p the port
     * @param path default the path
     * @param user the user name ({@code null} to skip the the authentication)
     * @param pwd the password (will be ignored if user is {@code null})
     */
    public FtpWuicResourceProtocol(final Boolean ftps, final String host, final int p, final String path, final String user, final String pwd) {
        ftpClient = ftps ? new FTPSClient(Boolean.FALSE) : new FTPClient();
        hostName = host;
        userName = user;
        password = pwd;
        port = p;
        basePath = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listResourcesPaths(final Pattern pattern) throws IOException {
        connect();
        return recursiveSearch(basePath, pattern);
    }

    /**
     * <p>
     * Opens a connection with the current FtpClient if its not already opened.
     * </p>
     *
     * @throws IOException if any I/O error occurs or if the credentials are not correct
     */
    private void connect() throws IOException {
        if (!ftpClient.isConnected()) {
            if (log.isDebugEnabled()) {
                log.debug("Connecting to FTP server.");
            }

            ftpClient.connect(hostName, port);

            if (userName != null && !ftpClient.login(userName, password)) {
                throw new IOException("Bad FTP credentials.");
            }
        }
    }

    /**
     * <p>
     * Searches recursively in the given path any files matching the given entry.
     * </p>
     *
     * @param path the path
     * @param pattern the pattern to match
     * @return the list of matching files
     * @throws IOException if the client can't move to a directory or any I/O error occurs
     */
    private List<String> recursiveSearch(final String path, final Pattern pattern) throws IOException {
        if (!ftpClient.changeWorkingDirectory(path)) {
            throw new IOException("Can move to the following directory : " + path);
        } else {
            final List<String> retval = new ArrayList<String>();

            // Test each file
            for (final FTPFile file : ftpClient.listFiles()) {
                final Matcher matcher = pattern.matcher(file.getName());

                if (matcher.find()) {
                   retval.add(matcher.group());
                }
            }

            // Search in each directory
            for (final FTPFile directory : ftpClient.listDirectories()) {
                retval.addAll(recursiveSearch(directory.getName(), pattern));
            }

            return retval;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResource accessFor(final String realPath, final FileType type) throws IOException {
        // Connect if necessary
        connect();

        ftpClient.changeWorkingDirectory(basePath);

        // Download file into memory
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(IOUtils.WUIC_BUFFER_LEN);
        IOUtils.copyStream(ftpClient.retrieveFileStream(realPath), baos);

        // Check if download is OK
        if (!ftpClient.completePendingCommand()) {
            throw new IOException("FTP command not completed correctly.");
        }

        // Create resource
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return new InputStreamWuicResource(bais, realPath, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Disconnecting from FTP server...");
            }

            // This object if not referenced and is going to be garbage collected.
            // Do not keep the client connected.
            ftpClient.disconnect();
        } finally {
            super.finalize();
        }
    }
}
