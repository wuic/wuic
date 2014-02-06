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


package com.github.wuic.nut.ftp;

import com.github.wuic.NutType;
import com.github.wuic.exception.PollingOperationNotSupportedException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.core.ByteArrayNut;
import com.github.wuic.util.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.nut.NutDao} implementation for FTP accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.3
 * @since 0.3.1
 */
public class FtpNutDao extends AbstractNutDao {

    /**
     * Expected format when retrieved last modification date.
     */
    private static final DateFormat MODIFICATION_TIME_FORMAT = new SimpleDateFormat("yyyyMMddhhmmss");

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
     * Use path as regex or not.
     */
    private Boolean regularExpression;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param ftps use FTPS or FTP protocol
     * @param host the host name
     * @param p the port
     * @param path default the path
     * @param basePathAsSysProp {@code true} if the base path is a system property
     * @param user the user name ({@code null} to skip the the authentication)
     * @param pwd the password (will be ignored if user is {@code null})
     * @param proxies proxy URIs serving the nut
     * @param pollingSeconds interleave in seconds for polling feature (-1 to disable)
     * @param regex consider path as regex or not
     */
    public FtpNutDao(final Boolean ftps,
                     final String host,
                     final int p,
                     final String path,
                     final Boolean basePathAsSysProp,
                     final String user,
                     final String pwd,
                     final String[] proxies,
                     final int pollingSeconds,
                     final Boolean regex) {
        super(path, basePathAsSysProp, proxies, pollingSeconds);
        ftpClient = ftps ? new FTPSClient(Boolean.TRUE) : new FTPClient();
        hostName = host;
        userName = user;
        password = pwd;
        port = p;
        regularExpression = regex;
    }

    /**
     * <p>
     * Opens a connection with the current FtpClient if its not already opened.
     * </p>
     *
     * @throws IOException if any I/O error occurs, the connection is refused or if the credentials are not correct
     */
    private void connect() throws IOException {
        if (!ftpClient.isConnected()) {
            log.debug("Connecting to FTP server.");

            ftpClient.connect(hostName, port);

            log.debug(ftpClient.getReplyString());

            // After connection attempt, you should check the reply code to verify success
            final int reply = ftpClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTP server refused connection.");
            }

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

            // Test each path
            for (final FTPFile file : ftpClient.listFiles()) {
                final Matcher matcher = pattern.matcher(file.getName());

                if (matcher.find()) {
                   retval.add(matcher.group());
                }
            }

            // Search in each directory
            for (final FTPFile directory : ftpClient.listDirectories()) {
                final String pwd = ftpClient.printWorkingDirectory();
                retval.addAll(recursiveSearch(directory.getName(), pattern));

                // Remove quotes around the path
                if (pwd.startsWith("\"") && pwd.endsWith("\"")) {
                    ftpClient.changeWorkingDirectory(new StringBuilder()
                            .append(pwd)
                            .deleteCharAt(pwd.length() - 1)
                            .deleteCharAt(0)
                            .toString());
                } else {
                     ftpClient.changeWorkingDirectory(pwd);
                }
            }

            return retval;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String realPath, final NutType type) throws StreamException {
        try {
            // Connect if necessary
            connect();

            ftpClient.changeWorkingDirectory(getBasePath());

            // Download path into memory
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(IOUtils.WUIC_BUFFER_LEN);
            IOUtils.copyStream(ftpClient.retrieveFileStream(realPath), baos);

            // Check if download is OK
            if (!ftpClient.completePendingCommand()) {
                throw new IOException("FTP command not completed correctly.");
            }

            // Create nut
            return new ByteArrayNut(baos.toByteArray(), realPath, type, new BigInteger(getLastUpdateTimestampFor(realPath).toString()));
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
        try {
            // Connect if necessary
            connect();

            log.info("Polling FTP nut '{}'", path);
            final String response = ftpClient.getModificationTime(IOUtils.mergePath(getBasePath(), path));
            log.info("Last modification response : {}", response);
            log.info("Parse the response with {} date format which could be preceded by the server code and a space",
                    MODIFICATION_TIME_FORMAT);

            return MODIFICATION_TIME_FORMAT.parse(response.substring(response.indexOf(' ') + 1)).getTime();
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        } catch (ParseException pe) {
            throw new PollingOperationNotSupportedException(this.getClass(), pe);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            log.debug("Disconnecting from FTP server...");

            // This object if not referenced and is going to be garbage collected.
            // Do not keep the client connected.
            ftpClient.disconnect();
        } finally {
            super.finalize();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s with base path %s", getClass().getName(), getBasePath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final Nut nut) {
        // TODO : implement FTP upload
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean saveSupported() {
        // TODO : return true once save() is implemented
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> listNutsPaths(final String pattern) throws StreamException {
        try {
            connect();
            return recursiveSearch(getBasePath(), Pattern.compile(regularExpression ? pattern : Pattern.quote(pattern)));
        } catch (IOException ioe) {
            throw new StreamException(ioe);
        }
    }
}
