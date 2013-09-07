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


package com.github.wuic.nut.ssh;

import com.github.wuic.NutType;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.core.ByteArrayNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * <p>
 * A {@link com.github.wuic.nut.NutDao} implementation for SSH accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.4
 * @since 0.3.1
 */
public class SshNutDao extends AbstractNutDao {

    /**
     * The SSH session.
     */
    private Session session;

    /**
     * Path considered as regular expression or not.
     */
    private Boolean regularExpression;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param regex consider paths as regex or not
     * @param host the host name
     * @param p the port
     * @param path default the path
     * @param user the user name ({@code null} to skip the the authentication)
     * @param pwd the password (will be ignored if user is {@code null})
     * @param pollingInterleave the interleave for polling operations in seconds (-1 to deactivate)
     * @param proxyUris the proxies URIs in front of the nut
     */
    public SshNutDao(final Boolean regex,
                     final String host,
                     final Integer p,
                     final String path,
                     final Boolean basePathAsSysProp,
                     final String user,
                     final String pwd,
                     final String[] proxyUris,
                     final int pollingInterleave) {
        super(path, basePathAsSysProp, proxyUris, pollingInterleave);
        regularExpression = regex;

        final JSch jsch = new JSch();

        try {
            session = jsch.getSession(user, host, p);
            session.setPassword(pwd);
            final Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
        } catch (JSchException je) {
            throw new IllegalStateException("Can't open SSH session", je);
        }
    }

    /**
     * <p>
     * Connects the current session.
     * </p>
     *
     * @throws JSchException if connection fails
     */
    private void connect() throws JSchException {
        if (!session.isConnected()) {
            session.connect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<String> listNutsPaths(final String pattern) throws StreamException {
        try {
            connect();

            if (regularExpression) {
                final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();
                channel.cd(getBasePath());
                final List<ChannelSftp.LsEntry> list = channel.ls(pattern);
                final List<String> retval = new ArrayList<String>(list.size());

                for (final ChannelSftp.LsEntry entry : list) {
                    retval.add(entry.getFilename());
                }

                return retval;
            } else {
                return Arrays.asList(pattern);
            }
        } catch (JSchException je) {
            throw new StreamException(new IOException(je));
        } catch (SftpException se) {
            throw new StreamException(new IOException(se));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Nut accessFor(final String path, final NutType type) throws StreamException {
        ChannelSftp channel = null;

        try {
            connect();

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.cd(getBasePath());
            final ByteArrayOutputStream os = new ByteArrayOutputStream(IOUtils.WUIC_BUFFER_LEN);
            channel.get(path, os);
            return new ByteArrayNut(os.toByteArray(), path, type);
        } catch (JSchException je) {
            throw new StreamException(new IOException("Can't load the file remotely with SSH FTP", je));
        } catch (SftpException se) {
            throw new StreamException(new IOException("An SSH FTP error prevent remote file loading", se));
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
        try {
            final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            channel.cd(getBasePath());
            return (long) channel.stat(path).getMTime();
        } catch (JSchException je) {
            throw new StreamException(new IOException("Can't load the file remotely with SSH FTP", je));
        } catch (SftpException se) {
            throw new StreamException(new IOException("Can't load the file remotely with SSH FTP", se));
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void finalize() throws Throwable {

        // Disconnect the session if this instance is not referenced anymore
        if (session.isConnected()) {
            session.disconnect();
        }

        super.finalize();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s with base path %s", getClass().getName(), getBasePath());
    }
}
