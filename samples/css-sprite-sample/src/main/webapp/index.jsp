<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Sample CSS sprites</title>

      <%@ taglib prefix="wuic" uri="http://www.github.com/wuic" %>
      <wuic:html-import workflowId="img" />
  </head>
  <body>
    <h2>Monitor your network traffic in your browser to see that only one image is loaded !</h2>

    <div>
        <span>Span with 'img_ax_icon' class :</span>
        <span class="img_ax_icon"></span>
    </div>

    <div>
        <span>Span with 'img_bv_icon' class :</span>
        <span class="img_bv_icon"></span>
    </div>

    <div>
        <span>Span with 'img_dk_icon' class :</span>
        <span class="img_dk_icon"></span>
    </div>

    <div>
        <span>Span with 'img_fi_icon' class :</span>
        <span class="img_fi_icon"></span>
    </div>

    <div>
        <span>Span with 'img_se_icon' class :</span>
        <span class="img_se_icon"></span>
    </div>
  </body>
</html>