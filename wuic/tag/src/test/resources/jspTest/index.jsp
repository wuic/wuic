<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <%@ taglib prefix="wuic-conf" uri="http://www.github.com/wuic/xml-conf" %>
    <wuic-conf:xml-configuration>
        <wuic>
            <nut-dao-builders>
                <nut-dao-builder id="cssDao" type="ClasspathNutDaoBuilder">
                    <properties>
                        <property key="c.g.wuic.dao.basePath">/jspTest</property>
                    </properties>
                </nut-dao-builder>
            </nut-dao-builders>
        </wuic>
    </wuic-conf:xml-configuration>

    <wuic-conf:xml-configuration>
        <wuic>
            <heaps>
                <heap id="foo" dao-builder-id="cssDao">
                    <nut-path>foo.css</nut-path>
                </heap>
            </heaps>
        </wuic>
    </wuic-conf:xml-configuration>

    <%@ taglib prefix="wuic" uri="http://www.github.com/wuic" %>
    <wuic:html-import workflowId="foo"/>
</head>
<body>
