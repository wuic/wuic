#!/bin/bash
mvn clean javadoc:aggregate -Pjavadoc && \
git checkout gh-pages && \
git rm -r apidocs && \
cp -r target/site/apidocs . && \
git add apidocs