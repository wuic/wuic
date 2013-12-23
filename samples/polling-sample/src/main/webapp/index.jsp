<%@ page import="com.github.wuic.util.IOUtils" %>
<%@ page import="java.io.ByteArrayOutputStream" %>
<%@ page import="java.io.FileInputStream" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="com.github.wuic.sample.polling.servlet.InitJavascriptFileListener" %>
<%@ page import="java.io.File" %>
<%@ page import="javax.xml.bind.JAXBContext" %>
<%@ page import="com.github.wuic.xml.XmlWuicBean" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <html>
    <head>
        <title>Polling sample</title>

        <%@ taglib prefix="wuic" uri="http://www.github.com/wuic" %>
        <wuic:html-import pageName="apps-js" />
        <wuic:html-import pageName="apps-css" />
    </head>
    <body>
        <h1 class="wuic">Polling sample</h1>
        <h2>
            In this sample, see how cache could be invalidated when polling operations detects an update
        </h2>
        <p>
            This code is loaded through WUIC :
            <ul>
                <li>The processed nut is put in a memory cache</li>
                <li>This cache is eternal, so the only way to see modifications is to invalidate the cache</li>
                <li>A thread polls the nut every 10 seconds and invalidate the cache if it has changed</li>
            </ul>
            You can modify, submit and refresh this code. You will see changes once WUIC has polled the nut containing this
            code and invalidated the cache. When WUIC polls the file and detect changes, it updates configurations.
        </p>

        <form action="/save" method="POST">
            <label for="script" style="vertical-align: top">Polled script :</label><br />
            <textarea id="script" name="script" cols="50" rows="25"><%
                try {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    final InputStream is = new FileInputStream(new File(InitJavascriptFileListener.DIRECTORY_PATH, InitJavascriptFileListener.FILE_NAME));
                    IOUtils.copyStream(is, bos);
                    out.print(new String(bos.toByteArray()));
                } catch (Exception e) {
                    out.print(e.getMessage());
                }
            %></textarea><br />
            <p>
                The wuic.xml file is also configured to be polled every 10 seconds. When you submit this form, the file
                is updated and the cache is toggled on/off regarding the checkbox value ('on' by default).
            </p>
            <label for="cache">Caching property in polled wuic.xml :</label>
            <input type="checkbox" id="cache" name="cache"<%
                try {
                    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    final JAXBContext ctx = JAXBContext.newInstance(XmlWuicBean.class);
                    final File file = new File(InitJavascriptFileListener.DIRECTORY_PATH, "wuic.xml");
                    final XmlWuicBean bean = (XmlWuicBean) ctx.createUnmarshaller().unmarshal(file);
                    final Boolean cache = Boolean.parseBoolean(bean.getEngineBuilders().get(0).getProperties().get(0).getValue());
                    out.print(cache ? " checked" : "");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            %> /><br />
            <input type="submit" />
        </form>
    </body>
</html>