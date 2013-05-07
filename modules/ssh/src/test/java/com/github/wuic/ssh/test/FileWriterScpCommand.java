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


package com.github.wuic.ssh.test;

import org.apache.sshd.server.command.ScpCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * SCP command for tests. A part of the source code has been picked from the {@code ScpCommand} itself
 * and adapted for the specific purpose of writing file from the disk to the client.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.3.1
 */
public class FileWriterScpCommand extends ScpCommand {

    /**
     * The file to be written.
     */
    private File file;

    /**
     * <p>
     * Builds a new {@code Command} in charge of sending the given file.
     * </p>
     *
     * @param f the file
     */
    public FileWriterScpCommand(final File f) {

        // Evict exception in super constructor
        super(new String[]{ "scp", "-f" });

        file = f;
    }

    /**
     * <p>
     * Reads the file and write it into the output stream.
     * </p>
     *
     * @throws IOException if any I/O error occurs
     */
    private void readFile() throws IOException {

        // Header
        final StringBuilder buf = new StringBuilder();
        buf.append("C");
        buf.append("0644"); // what about perms
        buf.append(" ");
        buf.append(file.length()); // length
        buf.append(" ");
        buf.append(file.getName());
        buf.append("\n");
        out.write(buf.toString().getBytes());
        out.flush();

        // Ack
        readAck();

        // Read the file and write it into output
        final InputStream is = new FileInputStream(file);
        com.github.wuic.util.IOUtils.copyStream(is, out);
        is.close();

        // Ack
        ack();
        readAck();
    }

    /**
     * <p>
     * Reads ACK.
     * </p>
     *
     * @throws IOException if warning or nack is received
     */
    private void readAck() throws IOException {
        int c = in.read();

        switch (c) {
            case 0:
                break;
            case 1:
                System.out.println("Received warning : " + readLine());
                break;
            case 2:
                throw new IOException("Received nack : " + readLine());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        try {

            // Try to read
            if (!file.exists()) {
                throw new IOException(file.getAbsolutePath() + ": no such file or directory");
            } else if (file.isFile()) {
                readFile();
            } else {
                throw new IOException(file.getAbsolutePath() + ": not a file");
            }
        } catch (IOException e) {

            // Write error
            try {
                out.write(2);
                out.write(e.getMessage().getBytes());
                out.write('\n');
                out.flush();
            } catch (IOException e2) {
                // Ignore
            }
            e.printStackTrace();
        } finally {

            // Tell the end of work
            callback.onExit(0);
        }
    }
}
