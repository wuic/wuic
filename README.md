## Welcome to WUIC (Web UI Compressor) 

#### The library which minifies your scripts (Javascript/CSS), computes your image's sprites and supports aggregation for you.

The library has been designed to guarantee that good practices around web resources will be always applied when you deploy in production.

Many websites have poor performances due to the many RTTs (round trip time) performed to load images, javascript and css files.
A lot of developers know that it is recommanded to aggregate those files to reduce the number of RTTs. Moreover, some tools like
YUICompressor can reduce the size of your CSS and Javascript files to speed up the page loading.

However, people do not apply those good practices. The truth is that it is a too expensive task in time in a continuous delivery. People don't the time
to aggregate the files, minify (in case of scripts) or create sprites (in case of images) and change the import statement in there HTML
code.

The purpose of WUIC is to help the developer to automate as much as possible these tasks with the maximum of flexibility to guarantee that the device will not be deprecated in the future.

License
====

  "Copyright (c) 2013   Capgemini Technology Services (hereinafter "Capgemini")
 
  License/Terms of Use
  Permission is hereby granted, free of charge and for the term of intellectual
  property rights on the Software, to any person obtaining a copy of this software
  and associated documentation files (the "Software"), to use, copy, modify and
  propagate free of charge, anywhere in the world, all or part of the Software
  subject to the following mandatory conditions:
 
  •   The above copyright notice and this permission notice shall be included in
  all copies or substantial portions of the Software.
 
  Any failure to comply with the above shall automatically terminate the license
  and be construed as a breach of these Terms of Use causing significant harm to
  Capgemini.
 
  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
  OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 
  Except as contained in this notice, the name of Capgemini shall not be used in
  advertising or otherwise to promote the use or other dealings in this Software
  without prior written authorization from Capgemini.
 
  These Terms of Use are subject to French law.
 
  IMPORTANT NOTICE: The WUIC software implements software components governed by
  open source software licenses (BSD and Apache) of which CAPGEMINI is not the
  author or the editor. The rights granted on the said software components are
  governed by the specific terms and conditions specified by Apache 2.0 and BSD
  licenses."



