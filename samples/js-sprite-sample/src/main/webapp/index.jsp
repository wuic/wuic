<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>Sample JS sprites (with cgSceneGraph)</title>

      <%@ taglib prefix="wuic" uri="http://www.github.com/wuic" %>
      <wuic:html-import pageName="img" />
      <wuic:html-import pageName="cgsg-js" />
  </head>
  <body>
      <canvas id="scene" width="1920" height="1080">
          Your browser does not support the canvas element.
      </canvas>

      <script type="text/javascript">
          // Get the canvas
          var canvasScene = document.getElementById("scene");

          // Create the scene
          var init = new CGSGScene(canvasScene);
          init.startPlaying();

          var aggregateUrl;
          var offsetY = 5;
          var maxW = 0;
          var root = new CGSGNode(0, 0, 0, 0);
          init.sceneGraph.addNode(root, null);
          var first = true;

          // Add each image to the scene
          for (var file in WUIC_SPRITE_IMG) {
              var sprite = WUIC_SPRITE_IMG[file];

              // Create image thanks to the provided sprite
              var img = new CGSGNodeImage(0, 0, sprite.url);
              img.setSlice(parseInt(sprite.x), parseInt(sprite.y), parseInt(sprite.w), parseInt(sprite.h), true);
              img.name = file;
              root.addChild(img);

              // Assume that aggregation is enabled : just get the original image and translate images once their dimensions are initialized
              if (first) {
                  first = false;
                  aggregateUrl = sprite.url;

                  img.onLoadEnd = function() {
                      new CGSGTraverser().traverse(root, function(node) {
                          if (node.classType == "CGSGNodeImage") {
                              var text = new CGSGNodeText(10, offsetY + node.getHeight() / 2, node.name + " : ");
                              text.setTextBaseline("middle");
                              root.addChild(text);
                              node.translateTo(text.position.x + text.getWidth() + 10, offsetY);
                              offsetY += node.getHeight() + 5;

                              if (node.position.x + node.getWidth() > maxW) {
                                  maxW = node.position.x + node.getWidth();
                              }
                          }

                          return false;
                      });

                      // Show the original image
                      var aggregateText = new CGSGNodeText(100 + maxW, 5, "Downloaded image : ");
                      root.addChild(aggregateText);
                      root.addChild(new CGSGNodeImage(aggregateText.position.x, 40, aggregateUrl));
                  };
              }
          }
      </script>
  </body>
</html>