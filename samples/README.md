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
            error in a compressed script !
            <b>
                NOTE : work in progress ! Already integrated the 'collapsible accordion' demo. Moreover, only JS could
                be imported because of the use of @import statement inside CSS files. [Imports will be supported later](https://github.com/gdrouet/wuic/issues/30).
            </b>
        </td>
        <td>
             JS compression. Cache. CSS (currently disabled).
        </td>
    </tr>
    </tr>
        <td>js-sprite-sample</td>
        <td>
            The sample includes the [cgSceneGraph](http://gwennaelbuchet.github.io/cgSceneGraph/) framework. It
            demonstrates how to include a set of images aggregated and loaded with sprite in Javascript. The demo
            just displays the different images using sprites.
        </td>
        <td>
             JS compression. Cache. JS sprites. Image aggregation.
        </td>
    </tr>
</table>
