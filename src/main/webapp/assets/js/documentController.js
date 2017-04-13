var billID = "";
var selectedLayer = null;

function setCarouselHeight() {
    var height = $(window).height();
    $("body").prepend("<style>.owl-item.active { overflow: auto; height:" + height + "px; }</style>");
};

function addLayerToContainer(layer, parent) {
    var newLayer = $("<div/>").addClass("indent").addClass(layer.attributes.part_type);
    var head = $("<h5/>").addClass("header").text(layer.attributes.header);
    var text = $("<p/>").addClass("text").text(layer.attributes.text);

    var toAdd = [];

    if(layer.header) head.prepend($("<span/>").addClass("enum").text(layer.attributes.enum));
    else text.prepend($("<span/>").addClass("enum").text(layer.attributes.enum));

    if(layer.attributes.header) toAdd.push(head);
    if(layer.attributes.text) toAdd.push(text);

    newLayer.append(toAdd).append(layerBtnSet(layer));

    if(layer.attributes.part_type != "quoted-block") {
        newLayer.on('mouseover', function(e){
            e.stopPropagation();
            $('.indent').removeClass('active');
            $(this).addClass("active");
        });
    }

    parent.append(newLayer);

    if(layer.children) {
        layer.children.map(function( obj ){
            addLayerToContainer( obj, newLayer );
        });
    }
};

function layerBtnSet( layer ) {
    var container = $('<div/>').addClass('btn-set');
    var btn = $('<a class="btn btn-primary"><i class="glyphicon glyphicon-cog"></i></a>');

    container.append([ btn ]);

    btn.on('click', function(){
        //$('#layer-tree').empty();
        $("#text").show();
        selectedLayer = layer;
        window.location.hash = layer.id;
        $("#layer-tree").tree('loadPart', billID, layer.id);
        $('.owl-carousel').trigger('to.owl.carousel', [1, 250]);
    });

    return container;
};

function findPart( search, id ) {
    var found = null;

    for(var x = 0, len = search.length; x < len; x++) {
        if(search[x].id == id) {
            found = search[x];
            break;
        }
        else if(search[x].children) {
            found = findPart( search[x].children, id );
            if(found) break;
        }
        else found = null;
    }

    return found;
};

$(document).ready(function(){
    $(".owl-carousel").owlCarousel({
        items: 1,
        mouseDrag: false,
        touchDrag: false,
        pullDrag: false,
        dots: false
    });

    $("#layer-tree").tree();

    $("#move").on("click", function(){
        $("#text").show();
        $(".owl-carousel").trigger("to.owl.carousel", [1, 250]);
    });

    $("#text").on("click", function(){
        $(this).hide();
        $(".owl-carousel").trigger("to.owl.carousel", [0, 250]);
    });

    $("#edit-node").on("local.setupEdit", function( event, node ){
        var modal = $("#generic-modal");

        $(this).off("click").on("click", function(){
            var template = $("#edit-form").clone().html();
            var body = modal.find(".modal-body");
            var title = modal.find(".modal-title");

            title.text("Edit Section Text");

            var commitBtn = modal.find("[name='generic-save']");

            body.empty().html(template);

            var header = body.find("input");
            var text = body.find("textarea");

            if(!node.attributes.statement || node.attributes.statement == "") {
                header.hide();
            } else {
                header.val(node.attributes.statement);
            }

            text.val(node.attributes.details);
            commitBtn.off("click");

            commitBtn.on("click", function(){
                var newNode = {
                    "details": text.val()
                };

                if(header.val().length > 0) newNode["statement"] = header.val();

                if(newNode.details.length > 0) {
                    $.ajax({
                        url: "/api/v1/bills/" + billID + "/parts/" + selectedLayer.id + "/nodes/" + node.id,
                        method: "PUT",
                        dataType: "json",
                        contentType: "application/json",
                        data: JSON.stringify(newNode)
                    }).done(function( json ){
                        if(!node.children) node.children = [json.data];
                        else node.children.push(json.data);

                        $("#layer-tree").tree("update", node);
                        $("#layer-tree").tree("selectNode", json.data);
                    }).fail(function( jqXHR ){
                        var modal = $("#confirm-modal");
                        var title = modal.find(".modal-title");
                        var body = modal.find(".modal-body");

                        title.text(jqXHR.statusText);
                        body.text(jqXHR.responseText);

                        modal.modal('show');
                    });
                }

                modal.modal('hide');
            });

            modal.modal('show');
        });
    });

    $("#comment-btn").on("local.setup", function( event, node ){
        var modal = $("#generic-modal");

        $(this).off("click").on("click", function(){
            var template = $("#comment-form").clone().html();
            var body = modal.find(".modal-body");
            var title = modal.find(".modal-title");
            var commitBtn = modal.find("[name='generic-save']");

            title.text("Add Comment");
            body.empty().html(template);
            var text = body.find("textarea");

            commitBtn.off("click").on("click", function(){
                var newComment = {
                    "text": text.val()
                };

                if(newComment.text.length > 0) {
                    $.ajax({
                        url: "/api/v1/bills/" + billID + "/parts/" + selectedLayer.id + "/nodes/" + node.id + "/comments",
                        method: "PUT",
                        dataType: "json",
                        contentType: "application/json",
                        data: JSON.stringify(newComment)
                    }).done(function( json ){
                        $("#layer-tree").tree("addComment", json.data);
                    }).fail(function( jqXHR ){
                        var modal = $("#confirm-modal");
                        var title = modal.find(".modal-title");
                        var body = modal.find(".modal-body");

                        title.text(jqXHR.statusText);
                        body.text(jqXHR.responseText);

                        modal.modal('show');
                    });
                }

                modal.modal('hide');
            });

            modal.modal('show');
        });

    });

    setCarouselHeight();

    $("#share-fb").on("click",function(){
        var fbpopup = window.open("https://www.facebook.com/sharer/sharer.php?u="+encodeURIComponent(window.location.href), "pop", "width=600, height=400, scrollbars=no");
        return false;
    });

    $("#share-tw").on("click",function(){
        var fbpopup = window.open("https://twitter.com/intent/tweet?url="+encodeURIComponent(window.location.href)+"&hashtags=becongress", "pop", "width=600, height=400, scrollbars=no");
        return false;
    });

    var path = window.location.pathname;
    var match = path.match(/.+\/(.+?)$/);

    if(match.length > 1) {
        billID = match[1];

        var path = "/api/v1/bills/" + billID + "?include=parts";

        $.ajax({
            url: path,
            method: "GET",
            dataType: "json"
        }).done(function( json ){
            if(json.data) {
                var attr = json.data.attributes;
                var btn = $('<a class="btn btn-success" target="_blank" id="download">PDF Text</a>');
                btn.attr('href', attr.pdf);

                $('#bill-title').text(attr.official_title);
                $('#bill-data').append(btn);

                if(json.data.relationships) {
                    var container = $("#bill-text");

                    json.data.relationships.parts.map(function( obj ){
                        addLayerToContainer( obj, container );
                    });

                    var hash = window.location.hash.replace(/^#/, "");
                    if(hash.length > 0) {
                        var split = hash.split("::");
                        var found = findPart(json.data.relationships.parts, split[0]);

                        if(found) {
                            $("#text").show();
                            selectedLayer = found;
                            $("#layer-tree").tree('loadPart', billID, found.id);
                            $('.owl-carousel').trigger('to.owl.carousel', [1, 250]);
                        }
                    }
                }
            }
        });
    }
});