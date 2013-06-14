/**
 * this class represent an Image Factory for WUIC
 */
var WUICCGSGNodeImageFactory = CGSGObject.extend ({

    initialize : function (groupId) {
        this.wuicData = eval("WUIC_SPRITE_" + groupId.toUpperCase());
        this.imgMap = new CGSGMap();

        for (var file in this.wuicData) {
            var sprite = this.wuicData[file];
            this.imgMap.addOrReplace(file, sprite.url);
        }
    },

    create : function(name, data) {
        var sprite = this.wuicData[name];

        // Create image thanks to the provided sprite
        var img = this.buildNode(data, sprite.url);
        img.setSlice(parseInt(sprite.x), parseInt(sprite.y), parseInt(sprite.w), parseInt(sprite.h), true);
        img.name = name;
        return img;
    },

    buildNode : function(data, url) {
        return  new CGSGNodeImage(data.x, data.y, url);
    },

    getImgMap : function () {
        return this.imgMap;
    }
});


