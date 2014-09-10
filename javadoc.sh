#!/bin/bash
cd wuic && \
mvn clean javadoc:aggregate -Pjavadoc && \
cd ../../wuic.github.io && \
git rm -r apidocs && \
cp -r ../web-ui-compressor/wuic/target/site/apidocs . && \
git add apidocs