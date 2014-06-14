The table below describes every samples of the WUIC project. Each sample is a module defined in 'samples'.
Samples are not hosted in maven central. To use them, simply download the project's archive on github, move
into the desired sample module and just run :

```
  mvn jetty:run
```

Some samples are currently available on the 'SNAPSHOT' branche. However, you can use them until they will be released.

<table width=100% height=100%>
    <tr>
        <td>Name</td>
        <td>Description</td>
        <td>Features demonstrated</td>
    </tr>
        <td>bootstrap3-sample</td>
        <td>
            This sample embeds the bootstrap3 samples and applies the Servlet filter provided by WUIC.
        </td>
        <td>
             YuiCompressor Javascript & CSS minification support. Script aggregation. Servlet filter.
        </td>
    </tr>
    <tr>
        <td>js-css-sample</td>
        <td>
            The webapp embeds the <a href="http://jqueryui.com/resources/download/jquery-ui-1.10.2.zip">JQuery UI</a> archive.
            All the demos have been copied into the '/using-wuic' path to show how to integrate them with WUIC.
            This way, you can see how you can embed uncompressed framework resources and configure a 'production' mode
            using WUIC. Think how it would be useful to disable compression to debug when your application raises an
            error in a compressed script!
            <b>
                NOTE : work in progress! Already integrated the different 'accordion', 'addClass', 'animate', 'autocomplete', 'button', 'datepicker' demos.
            </b>

            You can also see how the servlet filter works under the path '/jquery-ui-1.10.2/filter'.
        </td>
        <td>
             YuiCompressor Javascript & CSS minification support. EhCache support. Script aggregation. Servlet filter.
        </td>
    </tr>
    <tr>
        <td>js-sprite-sample</td>
        <td>
            The sample includes the <a href="http://gwennaelbuchet.github.io/cgSceneGraph/">cgSceneGraph</a> framework.
            It demonstrates how to include a set of images aggregated and loaded with sprite in Javascript. The demo
            just displays the different images using sprites.
        </td>
        <td>
             YuiCompressor Javascript minification support. EhCache support. JS sprites. Image aggregation. Script aggregation.
        </td>
    </tr>
    <tr>
        <td>css-sprite-sample</td>
        <td>
            Demonstrates how to include a set of images aggregated and loaded with sprite in CSS. The demo
            just displays a set of different flags loaded from a single image .
        </td>
        <td>
             Cache. CSS sprites. Image aggregation.
        </td>
    </tr>
    <tr>
        <td>thymeleaf-sample</td>
        <td>
            With a modified demo from <a herf="http://datatables.net/">datatable</a> project, this samples shows how you
            can use the thymeleaf dialect and its import processor.
        </td>
        <td>
             Thymeleaf support. Memory Cache. Script aggregation.
        </td>
    </tr>
    <tr>
        <td>polling-sample</td>
        <td>
            Demonstrates how WUIC can poll both configuration files and nuts and refresh them at runtime.
        </td>
        <td>
             Memory Cache. Configuration polling. Nut polling.
        </td>
    </tr>
    <tr>
        <td>build-time-sample</td>
        <td>
            This sample shows how you can process nuts with WUIC when you build your project with maven and not on the fly.
            The application is based on a demo from the famous <a href="https://github.com/madebymany/sir-trevor-js">Sir Trevor</a>
            project.
        </td>
        <td>
             Plugin 'static-helper-maven-plugin' for maven.
        </td>
    </tr>
</table>
