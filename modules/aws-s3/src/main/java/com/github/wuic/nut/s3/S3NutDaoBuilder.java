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


package com.github.wuic.nut.s3;

import com.github.wuic.ApplicationConfig;
import com.github.wuic.exception.BuilderPropertyNotSupportedException;

import com.github.wuic.nut.AbstractNutDaoBuilder;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.setter.*;
import com.github.wuic.nut.setter.ProxyUrisPropertySetter;

/**
 * <p>
 * Builder for nut access on a S3 AWS Cloud.
 * </p>
 *
 * @author Corentin AZELART
 * @version 1.2
 * @since 0.3.3
 */
public class S3NutDaoBuilder extends AbstractNutDaoBuilder {

    /**
     * <p>
     * Creates a new instance.
     * </p>
     */
    public S3NutDaoBuilder() {
        super();
        addPropertySetter(new BucketPropertySetter(this, null),
                new ProxyUrisPropertySetter(this),
                new PollingInterleavePropertySetter(this),
                new BasePathPropertySetter(this, ""),
                new BasePathAsSysPropPropertySetter(this),
                new LoginPropertySetter(this, null),
                new PasswordPropertySetter(this, ""),
                new BucketPropertySetter(this, ""),
                new RegexPropertySetter(this));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NutDao internalBuild() throws BuilderPropertyNotSupportedException {
        return new S3NutDao(
                (String) property(ApplicationConfig.BASE_PATH),
                (Boolean) property(ApplicationConfig.BASE_PATH_AS_SYS_PROP),
                (String[]) property(ApplicationConfig.PROXY_URIS),
                (Integer) property(ApplicationConfig.POLLING_INTERLEAVE),
                (String) property(ApplicationConfig.CLOUD_BUCKET),
                (String) property(ApplicationConfig.LOGIN),
                (String) property(ApplicationConfig.PASSWORD),
                (Boolean) property(ApplicationConfig.REGEX));
    }
}