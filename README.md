## Welcome to WUIC (Web UI Compressor) 

#### The library which minifies your scripts (Javascript/CSS), computes your image's sprites and supports aggregation for you.

The library has been designed to guarantee that good practices around web resources will be always applied when you deploy in production.

Many websites have poor performances due to the many RTTs (round trip time) performed to load images, javascript and css files.
A lot of developers know that it is recommanded to aggregate those files to reduce the number of RTTs. Moreover, some tools like
YUICompressor can reduce the size of your CSS and Javascript files to speed up the page loading.

However, people do not apply those good practices. The truth is that it is a too expensive task in time in a continuous delivery. People don't have the time
to aggregate the files, minify (in case of scripts) or create sprites (in case of images) and change the import statement in there HTML
code.

The purpose of WUIC is to help the developer to automate as much as possible these tasks with the maximum of flexibility to guarantee that the device will not be deprecated in the future.

Release
====

Our current release is the [v0.3.0](https://github.com/gdrouet/wuic/issues?labels=0.3.0&milestone=&page=1&state=closed) !

Prior to v0.3.0, WUIC was not on github. See the old change logs (here)[https://raw.github.com/gdrouet/wuic/master/changelog.txt]

You can integrate the library using this maven dependency (available on the [maven central repository](http://search.maven.org/#search|ga|1|wuic)) :

```xml
     <dependency>
         <groupId>com.github.wuic</groupId>
         <artifactId>wuic-{wuic-module}</artifactId>
         <version>0.3.0</version>
      </dependency>
```

If your webapp is based on JSPs, the easiest way to use WUIC is to add the wuic-tag artifact in your pom.xml.
Otherwise, you can just depend on the wuic-servlet artifact.

The next major release is targeted to the end of June. Intermediate releases are already in the roadmap and more documentation is going to be written in the next weeks !

Quick start
====

The best way to see how WUIC works and how it could help you is to [download](https://github.com/gdrouet/wuic/archive/master.zip) the source code of the project.

Then you can look at our different [samples](https://github.com/gdrouet/wuic/tree/master/samples).

You can see an [example](https://github.com/gdrouet/wuic/tree/master/samples/js-sprite-sample) showing how WUIC aggregates images, computes sprites and minifies Javascript for you !

Just move to the sample's directory run it with :

```
mvn jetty:run
```
License
====

You will find everything about the WUIC's license [here](https://github.com/gdrouet/wuic/wiki/License).
