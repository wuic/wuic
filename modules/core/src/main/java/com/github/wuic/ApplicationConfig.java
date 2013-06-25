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


package com.github.wuic;

/**
 * <p>
 * All the application configurations supported by WUIC are defined here.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.3.1
 */
public interface ApplicationConfig {

    /////////////////////////////////////////////////
    ////// ClasspathWuicResourceFactory properties ///////
    /////////////////////////////////////////////////

    /**
     * The base path when accessing resources on the classpath.
     */
    String CLASSPATH_BASE_PATH = "c.g.wuic.classpathBasePath";


    /////////////////////////////////////////////////
    ////// DiskWuicResourceFactory properties ///////
    /////////////////////////////////////////////////

    /**
     * The base path when accessing resources on the file system.
     */
    String DISK_BASE_PATH = "c.g.wuic.diskBasePath";

    /////////////////////////////////////////////////
    ////// WebappWuicResourceFactory properties ///////
    /////////////////////////////////////////////////

    /**
     * The base path when accessing resources deployed in the webapp on file system.
     */
    String WEBAPP_BASE_PATH = "c.g.wuic.webappBasePath";

    /////////////////////////////////////////////////
    ////// HttpWuicResourceFactory properties ///////
    /////////////////////////////////////////////////

    /**
     * The server's domain when accessing resources over HTTP.
     */
    String HTTP_SERVER_DOMAIN = "c.g.wuic.httpServerDomain";

    /**
     * Use HTTPS or not accessing resources over HTTP.
     */
    String HTTP_SECRET_PROTOCOL = "c.g.wuic.httpS";

    /**
     * The server's port when accessing resources over HTTP.
     */
    String HTTP_SERVER_PORT = "c.g.wuic.httpServerPort";

    /**
     * The server's base path when accessing resources over HTTP.
     */
    String HTTP_SERVER_BASE_PATH = "c.g.wuic.httpBasePath";

    /////////////////////////////////////////////////
    ////// FtpWuicResourceFactory properties ///////
    /////////////////////////////////////////////////

    /**
     * The server's domain when accessing resources over FTP.
     */
    String FTP_SERVER_DOMAIN = "c.g.wuic.ftpServerDomain";

    /**
     * Use FTPS or not accessing resources over FTP.
     */
    String FTP_SECRET_PROTOCOL = "c.g.wuic.ftpS";

    /**
     * The server's port when accessing resources over FTP.
     */
    String FTP_SERVER_PORT = "c.g.wuic.ftpServerPort";

    /**
     * The server's port when accessing resources over FTP.
     */
    String FTPS_SERVER_PORT = "c.g.wuic.ftpsServerPort";

    /**
     * The server's base path when accessing resources over FTP.
     */
    String FTP_SERVER_BASE_PATH = "c.g.wuic.ftpBasePath";

    /**
     * The user name to use when accessing resources over FTP.
     */
    String FTP_USERNAME = "c.g.wuic.ftpUsername";

    /**
     * The password to use when accessing resources over FTP.
     */
    String FTP_PASSWORD = "c.g.wuic.ftpPassword";

    /////////////////////////////////////////////////
    ////// SshWuicResourceFactory properties ///////
    /////////////////////////////////////////////////

    /**
     * The server's domain when accessing resources over SSH.
     */
    String SSH_SERVER_DOMAIN = "c.g.wuic.sshServerDomain";

    /**
     * The server's port when accessing resources over SSH.
     */
    String SSH_SERVER_PORT = "c.g.wuic.sshServerPort";

    /**
     * The server's base path when accessing resources over SSH.
     */
    String SSH_SERVER_BASE_PATH = "c.g.wuic.sshBasePath";

    /**
     * Consider the base path as a system property associated to actual value.
     */
    String SSH_SERVER_BASE_PATH_AS_SYS_PROP = "c.g.wuic.sshBasePathAsSystemProperty";

    /**
     * The user name to use when accessing resources over SSH.
     */
    String SSH_USERNAME = "c.g.wuic.sshUsername";

    /**
     * The password to use when accessing resources over SSH.
     */
    String SSH_PASSWORD = "c.g.wuic.sshPassword";

    /**
     * The interpreter of the command sent via SSH (cmd, shell, etc).
     */
    String SSH_INTERPRETER = "c.g.wuic.sshInterpreter";

    /**
     * After the command is executed, the thread will sleep a time associated to this property.
     */
    String SSH_TIME_TO_SLEEP_AFTER_EXEC = "c.g.wuic.sshTimeToSleepAfterExec";

    /////////////////////////////////////////////////
    ////// S3WuicResourceFactory properties ///////
    /////////////////////////////////////////////////

    /**
     * The S3 AWS Bucket storage name.
     */
    String S3_BUCKET = "c.g.wuic.s3Bucket";

    /**
     * S3 access key.
     */
    String S3_KEY_ACCESS = "c.g.wuic.s3Key";

    /**
     * S3 secret key.
     */
    String S3_KEY_SECRET = "c.g.wuic.s3Secret";

    /**
     * S3 path for resources.
     */
    String S3_PATH = "c.g.wuic.s3Path";

    /////////////////////////////////////////////////
    ////// GStorageWuicResourceFactory properties ///////
    /////////////////////////////////////////////////

    /**
     * The Google Storage Bucket name.
     * <p>
     * You can set in your Google Cloud Console
     * https://cloud.google.com/console AWS Bucket storage name.
     * </p>
     */
    String GS_BUCKET = "c.g.wuic.gStorageBucket";

    /**
     * The Google Client ID.
     * <p>
     * The Google Cloud API client id, you can generate a new key on this page :
     * https://code.google.com/apis/console/
     * - Go on : API Access > "Create another client ID..."
     * - Installed application type : select "Service account"
     * - Copy here the "Email address" field, "xxxxx-yyyyy@developer.gserviceaccount.com
     * </p>
     */
    String GS_CLIENT_ID = "c.g.wuic.gStorageClientId";

    /**
     * The Google Storage path.
     * <p>
     * The Google Storage path for search resources without bucket name, example :
     * empty : search in root and sub directories
     * wuic : search in "wuic/*"
     * wuic/test : search in "wuic/test/*"
     * </p>
     */
    String GS_PATH = "c.g.wuic.gStoragePath";

    /**
     * Key : c.g.wuic.gPrivateKeyPath
     * You can generate a new key path on this page : https://code.google.com/apis/console/
     * - Go on : API Access > "Create another client ID..."
     * - Installed application type : select "Service account"
     * - Download key path or Generate new key
     */
    String GS_PRIVATE_KEY_PATH = "c.g.wuic.gPrivateKeyPath";
}
