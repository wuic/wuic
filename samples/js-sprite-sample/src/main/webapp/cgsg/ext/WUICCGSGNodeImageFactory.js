/**
 * this class represent an Image Factory for WUIC.
 */
var WUICCGSGNodeImageFactory = CGSGObject.extend ({

    initialize : function (groupId) {
        this.groupId = groupId.replace(/[^a-zA-Z]/g, '_');
        this.imgMap = new CGSGMap();
        var file, sprite;

        for (file in WUIC_SPRITE) {
            if (WUIC_SPRITE.hasOwnProperty(file) && file.substring(0, this.groupId.length) == this.groupId) {
                sprite = WUIC_SPRITE[file];
                this.imgMap.addOrReplace(file, sprite.url);
            }
        }
    },

    create : function(name, data) {
        var sprite = WUIC_SPRITE[name];

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


