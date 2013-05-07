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


package com.github.wuic.ssh;

import com.github.wuic.FileType;
import com.github.wuic.resource.WuicResource;
import com.github.wuic.resource.WuicResourceProtocol;
import com.github.wuic.resource.impl.InputStreamWuicResource;
import com.github.wuic.util.IOUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * <p>
 * A {@link com.github.wuic.resource.WuicResourceProtocol} implementation for SSH accesses.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.1
 */
public class SshWuicResourceProtocol implements WuicResourceProtocol {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The base path where to move.
     */
    private String basePath;

    /**
     * The SSH session.
     */
    private Session session;

    /**
     * The command manager.
     */
    private SshCommandManager sshCommandManager;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param host the host name
     * @param p the port
     * @param path default the path
     * @param user the user name ({@code null} to skip the the authentication)
     * @param pwd the password (will be ignored if user is {@code null})
     * @param command the command manager
     */
    public SshWuicResourceProtocol(final String host,
                                   final int p,
                                   final String path,
                                   final String user,
                                   final String pwd,
                                   final SshCommandManager command) {
        basePath = path;
        sshCommandManager = command;

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
     * <p>
     * Executes the given command in a remote shell.
     * </p>
     *
     * @param command the command to execute
     * @throws IOException if an I/O error occurs
     * @throws JSchException if the channel could not be opened
     */
    private void executeShellCommand(final String command) throws IOException, JSchException {
        // Open channel
        final Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        final OutputStream out = channel.getOutputStream();
        final InputStream in = channel.getInputStream();

        channel.connect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listResourcesPaths(final Pattern pattern) throws IOException {
        try {
            connect();

            // Create a file containing all the files
            final List<String> retval = new ArrayList<String>();
            String file = basePath + "wuic-list-resources-" + System.nanoTime();
            final String command = StringUtils.join(sshCommandManager.searchInto(basePath, pattern.pattern(), file), "\n");

            // Execute command and wait for end of execution
            executeShellCommand(command);

            // TODO : add this value as config variable in wuic.xml
            Thread.sleep(3000L);

            // Now read generate file
            final BufferedReader br = new BufferedReader(new InputStreamReader(loadFile(file)));

            // Each line is a candidate resource
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    retval.add(line);
                }
            }

            return retval;
        } catch (JSchException je) {
            throw new IOException(je);
        } catch (InterruptedException ie) {
            throw new IOException("Interrupted before reading the file", ie);
        }
    }

    /**
     * <p>
     * Loads the given file from SSH server into memory byte array.
     * </p>
     *
     * @param path the path
     * @return a stream pointing to the memory byte array
     * @throws JSchException if we can't open the channel
     * @throws IOException if we can't read the file
     */
    private InputStream loadFile(final String path) throws JSchException, IOException {
        // exec 'scp -f rfile' remotely
        final String command = new StringBuilder("scp -r -f \"").append(path).append("\"").toString();

        // Open channel and create command
        final Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        final OutputStream out = channel.getOutputStream();
        final InputStream in = channel.getInputStream();

        try {
            channel.connect();

            // Buffer for reading
            final byte[] buf = new byte[IOUtils.WUIC_BUFFER_LEN];

            // send '\0'
            sendZero(buf, out);

            // Read information
            final ByteArrayOutputStream inMemory = new ByteArrayOutputStream();

            while (checkAck(in) == 'C') {

                // read '0644 '
                in.read(buf, 0, 5);

                // read file size
                long fileSize = 0L;

                while (in.read(buf, 0, 1) >= 0 && buf[0] != ' ') {
                    fileSize = fileSize * 10L + (long) (buf[0] - '0');
                }

                if (log.isDebugEnabled()) {
                    log.debug("File size : " + fileSize);
                }

                // read file
                for (int i = 0; ; i++) {
                    in.read(buf, i, 1);

                    if (buf[i] == (byte) 0x0a) {
                        if (log.isDebugEnabled()) {
                            log.debug("File : " + new String(buf, 0, i));
                        }

                        break;
                    }
                }

                // send '\0'
                sendZero(buf, out);

                // read a content of file
                int offset = buf.length < fileSize ? buf.length : (int) fileSize;

                while ((offset = in.read(buf, 0, offset)) >= 0 && fileSize != 0) {
                    inMemory.write(buf, 0, offset);
                    fileSize -= offset;
                    offset = buf.length < fileSize ? buf.length : (int) fileSize;
                }

                checkAck(in);

                // send '\0'
                sendZero(buf, out);
            }

            return new ByteArrayInputStream(inMemory.toByteArray());
        } finally {
            channel.disconnect();
        }
    }

    /**
     * <p>
     * Sends 0 to server.
     * </p>
     *
     * @param buffer buffer to use
     * @param outputStream stream to server
     * @throws IOException if write operation fails
     */
    private void sendZero(final byte[] buffer, final OutputStream outputStream) throws IOException {
        buffer[0] = 0;
        outputStream.write(buffer, 0, 1);
        outputStream.flush();
    }

    /**
     * <p>
     * Checks the ACK.
     * </p>
     *
     * @param in the stream to be read from server
     * @return the read byte
     * @throws IOException in case of error
     */
    private int checkAck(final InputStream in) throws IOException {
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        final int b = in.read();

        if (b <= 0) {
            return b;
        }

        // Error occurs
        if (b == 1 || b == 2) {
            final StringBuilder sb = new StringBuilder();
            int c;

            // Read error and use it as message in exception
            do {
                c = in.read();
                sb.append((char)c);
            } while (c != '\n');

            throw new IOException(sb.toString());
        }

        return b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WuicResource accessFor(final String realPath, final FileType type) throws IOException {
        try {
            connect();
            return new InputStreamWuicResource(loadFile(realPath), realPath, type);
        } catch (JSchException je) {
            throw new IOException("Can't load the file remotely with SCP", je);
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
}
