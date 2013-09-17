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
    <tr>
        <td>js-css-sample</td>
        <td>
            The webapp embeds the [JQuery UI](http://jqueryui.com/resources/download/jquery-ui-1.10.2.zip) archive.
            All the demos have been copied into the '/using-wuic' path to show how to integrate them with WUIC.
            This way, you can see how you can embed uncompressed framework resources and configure a 'production' mode
            using WUIC. Think how it would be useful to disable compression to debug when your application raises an
            error in a compressed script!
            <b>
                NOTE : work in progress! Already integrated the different 'accordion', 'addClass', 'animate', 'autocomplete', 'button', 'datepicker' demos.
            </b>
        </td>
        <td>
             YuiCompressor Javascript & CSS minification support. EhCache support.
        </td>
    </tr>
    <tr>
        <td>js-sprite-sample</td>
        <td>
            The sample includes the [cgSceneGraph](http://gwennaelbuchet.github.io/cgSceneGraph/) framework. It
            demonstrates how to include a set of images aggregated and loaded with sprite in Javascript. The demo
            just displays the different images using sprites.
        </td>
        <td>
             YuiCompressor Javascript minification support. EhCache support. JS sprites. Image aggregation.
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
        <td>polling-sample</td>
        <td>
            Demonstrates how WUIC can poll both configuration files and nuts and refresh them at runtime.
        </td>
        <td>
             Memory Cache. Configuration polling. Nut polling.
        </td>
    </tr>
</table>