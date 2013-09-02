<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Sample JS sprites (with cgSceneGraph)</title>

      <%@ taglib prefix="wuic" uri="http://www.github.com/wuic" %>
      <wuic:html-import pageName="js-img" />
      <wuic:html-import pageName="js-cgsg-js" />
  </head>
  <body>

      <canvas id="scene" width="1920" height="1080">
            Your browser does not support the canvas element.
      </canvas>

      <script type="text/javascript">
          // Get the canvas
          var canvasScene = document.getElementById("scene");

          // Create the scene
          var main = new CGSGView(canvasScene);
          //resize the canvas to fulfill the viewport
          this.viewDimension = cgsgGetRealViewportDimension();
          main.setCanvasDimension(this.viewDimension);
          main.startPlaying();

          var aggregateUrl;
          var offsetY = 5;
          var maxW = 0;
          var root = new CGSGNode(0, 0, 0, 0);
          CGSG.sceneGraph.addNode(root, null);

          //Create the Node Image Factory with his specific groupId
          var imageFactory = new WUICCGSGNodeImageFactory("img");

          //get the map between imgUrl and spriteUrl
          var imgMap = imageFactory.getImgMap();

          //init property of the nodes
          var data = {};
          data.x = 0;
          data.y =  0;

          // Add each image to the scene
          for (var i = 0; i < imgMap.getLength(); i++) {

              //create the CGSGNodeImage with the WUIC factory
              var node = imageFactory.create(imgMap.getAt(i).key, data);

              //create a textNode for the img name
              var text = new CGSGNodeText(10, offsetY + node.getHeight() / 2, node.name + " : ");
              text.setTextBaseline("middle");
              root.addChild(text);

              node.translateTo(text.position.x + text.getWidth() + 10, offsetY);
              root.addChild(node);

              offsetY += node.getHeight() + 5;

              if (node.position.x + node.getWidth() > maxW) {
                  maxW = node.position.x + node.getWidth();
              }
          }

          var aggregateText = new CGSGNodeText(100 + maxW, 5, "Downloaded image : ");

          // Show the original image
          var imgAggregate = imageFactory.buildNode({x : aggregateText.position.x, y : 40}, imgMap.getAt(0).value);
          root.addChild(aggregateText);
          root.addChild(imgAggregate);
      </script>
  </body>
</html>