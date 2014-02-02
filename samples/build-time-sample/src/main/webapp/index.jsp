<!doctype html>
<!--[if lt IE 7]> <html class="no-js lt-ie9 lt-ie8 lt-ie7" lang="en"> <![endif]-->
<!--[if IE 7]>    <html class="no-js lt-ie9 lt-ie8" lang="en"> <![endif]-->
<!--[if IE 8]>    <html class="no-js lt-ie9" lang="en"> <![endif]-->
<!--[if gt IE 8]><!--> <html class="no-js" lang="en"> <!--<![endif]-->
<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title></title>
    <meta name="description" content="">
    <meta name="viewport" content="width=device-width">

    <%@ taglib prefix="wuic" uri="http://www.github.com/wuic" %>
    <wuic:html-import workflowId="css" />
</head>
<body>
<form>
    <div class="errors"></div>
    <textarea class="sir-trevor" name="content"></textarea>
</form>

<wuic:html-import workflowId="js" />

<script type="text/javascript" charset="utf-8">
    $(function(){

        SirTrevor.DEBUG = true;

        window.editor = new SirTrevor.Editor({
            el: $('.sir-trevor'),
            blockTypes: [
                "Embedly",
                "Text",
                "List",
                "Quote",
                "Image",
                "Video",
                "Tweet"
            ]
        });

        $('form').bind('submit', function(){
            return false;
        });

    });
</script>
</body>
</html>