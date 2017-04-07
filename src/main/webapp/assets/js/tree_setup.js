function setup_tree(bill, part) {
    var margin = {top: 20, right: 120, bottom: 20, left: 120},
        width = $("#layer-tree").width() - margin.right - margin.left,
        height = 800 - margin.top - margin.bottom;

    var i = 0,
        duration = 750,
        root;

    var tree = d3.layout.tree()
        .size([height, width]);

    var diagonal = d3.svg.diagonal()
        .projection(function(d) { return [d.y, d.x]; });

    var svg = d3.select("#layer-tree").append("svg")
        .attr("width", width + margin.right + margin.left)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    d3.json("/api/v1/bills/" + bill + "/parts/" + part + "?include=nodes", function(error, flare) {
        if (error) throw error;
        if(flare.data && flare.data.relationships && flare.data.relationships.nodes) {
            flare = flare.data.relationships.nodes;
        } else {
            flare = [];
        }

        if(flare.length > 0) {
            root = flare[0];
            root.x0 = height / 2;
            root.y0 = 0;

            /**
            *   collapse puts all children of all elements in a
            *   'hidden' space so as not to be rendered. to show
            *   all on load, comment out this area.
            */
            function collapse(d) {
                if (d.attributes.children && d.attributes.children.length > 0) {
                    d.attributes._children = d.attributes.children;
                    d.attributes._children.forEach(collapse);
                    d.attributes.children = null;
                }
            }

            if(root.attributes.children) root.attributes.children.forEach(collapse);
            update(root);
            setupNodeInfo(root);
        }
    });

    d3.select(self.frameElement).style("height", "800px");

    function update(source) {

      // Compute the new tree layout.
      var nodes = tree.nodes(root).reverse(),
          links = tree.links(nodes);

      // Normalize for fixed-depth.
      nodes.forEach(function(d) { d.y = d.depth * 180; });

      // Update the nodes…
      var node = svg.selectAll("g.node")
          .data(nodes, function(d) { return d.id || (d.id = ++i); });

      // Enter any new nodes at the parent's previous position.
      var nodeEnter = node.enter().append("g")
          .attr("class", "node")
          .attr("transform", function(d) { return "translate(" + source.y0 + "," + source.x0 + ")"; })
          .on("click", click);

      nodeEnter.append("circle")
          .attr("r", 1e-6)
          .style("fill", function(d) { return d._children ? "lightsteelblue" : "#fff"; });

      nodeEnter.append("text")
          .attr("x", function(d) { return d.attributes.children || d.attributes._children ? -10 : 10; })
          .attr("dy", ".35em")
          .attr("text-anchor", function(d) { return d.attributes.children || d.attributes._children ? "end" : "start"; })
          .text(function(d) { return d.id; })
          .style("fill-opacity", 1e-6);

      // Transition nodes to their new position.
      var nodeUpdate = node.transition()
          .duration(duration)
          .attr("class", function(d){ return d.id == source.id ? "node temp" : "node"; })
          .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; });

      nodeUpdate.select("circle")
          .attr("r", 6.5)
          .style("fill", function(d) { return d.attributes._children ? "lightsteelblue" : "#fff"; });

      nodeUpdate.select("text")
          .style("fill-opacity", 1);

      // Transition exiting nodes to the parent's new position.
      var nodeExit = node.exit().transition()
          .duration(duration)
          .attr("transform", function(d) { return "translate(" + source.y + "," + source.x + ")"; })
          .attr("class", "node")
          .remove();

      nodeExit.select("circle")
          .attr("r", 1e-6);

      nodeExit.select("text")
          .style("fill-opacity", 1e-6);

      // Update the links…
      var link = svg.selectAll("path.link")
          .data(links, function(d) { return d.target.id; })
          .attr("class", function(d){ return leadsToTarget(d.target, source) ? "link selected" : "link" });

      // Enter any new links at the parent's previous position.
      link.enter().insert("path", "g")
          .attr("class", "link")
          .attr("d", function(d) {
            var o = {x: source.x0, y: source.y0};
            return diagonal({source: o, target: o});
          });

      // Transition links to their new position.
      link.transition()
          .duration(duration)
          .attr("d", diagonal);

      // Transition exiting nodes to the parent's new position.
      link.exit().transition()
          .duration(duration)
          .attr("d", function(d) {
            var o = {x: source.x, y: source.y};
            return diagonal({source: o, target: o});
          })
          .remove();

      // Stash the old positions for transition.
      nodes.forEach(function(d) {
        d.x0 = d.x;
        d.y0 = d.y;
      });
    }

    // Toggle children on click.
    function click(d) {
        setupNodeInfo(d);

        if(d.id != root.id) {
            if (d.attributes.children) {
                d.attributes._children = d.attributes.children;
                d.attributes.children = null;
            } else {
                d.attributes.children = d.attributes._children;
                d.attributes._children = null;
            }
        }

        update(d);
    }

    function leadsToTarget( d, target ) {
        if(d.id == target.id) return true;
        else if(d.attributes.children && d.attributes.children.length > 0) {
            var fnd = false;
            for(var x = 0, len = d.attributes.children.length; x < len; x++) {
                if(leadsToTarget(d.attributes.children[x], target)) return true;
            }
        } else return false;
    }

    function setupNodeInfo( node ) {
        $("#title").find("[name='header']").text(node.attributes.statement);
        $("#title").find("[name='text']").text(node.attributes.details);
        $("#edit-node").trigger("local.setupEdit", [ node ] );

        setupNodeComments(node);
    }

    function setupNodeComments( node ) {
        $.ajax({
            url: "/api/v1/bills/" + bill + "/parts/" + part + "/nodes/" + node.id + "?include=comments",
            dataType: "json"
        }).done(function( json ){
            if(json.data && json.data.relationships && json.data.relationships.comments) {
                drawNodeComments( json.data.relationships.comments );
            }
        });
    }

    function drawNodeComments( comments ) {
        var container = $("#comment-id-root");
        container.empty();

        $.each(comments, function(index, value) {
            newNodeComment( value, container );

            $('[data-toggle="tooltip"]').tooltip()
        });

        $('.collapse.panel-body').collapse('show');
    }

    function newNodeComment( comment, parent ) {
        // if parent exists in DOM, insert into parent
        // if children exist in the DOM, wrap them into element

        var reply_button = $('<span class="glyphicon glyphicon-comment" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="reply">&nbsp;</span>');
        var downvote = $('<span class="glyphicon glyphicon-thumbs-down" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="vote down">&nbsp;</span>');
        var upvote = $('<span class="glyphicon glyphicon-thumbs-up" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="vote up">&nbsp;</span>');
        var vote = $('<span class="pull-right" style="margin-right:15px;font-size:12px;"></span>').append(comment.attributes.vote + " points");
        //vote.append(vote);

        reply_button.bind("click", $.proxy(this._newCommentReply, this));

        var downvotefunc = $.proxy(function(event){
            var element = $(event.target);
            var id = parent.attr('id').split('-');
            var self = this;
            $.post("/json/vote/comment/" + id[2], "vote=down").done(function(data) {
                //self.comments.push(data.comment);
                //self._newComment(data.comment.id, data.comment.parent, "username", data.comment.text);
                //event.data.remove();
                if(upvote.hasClass("uservote")) upvote.removeClass("uservote");
                downvote.addClass("uservote");
                vote.empty().append(data.comment.vote + " points");
                downvote.unbind("click");
                upvote.bind("click", upvotefunc);
            }).fail(function(){
                alert("An error occured. You must be logged in to use this function.");
            });
        }, this);

        var upvotefunc = $.proxy(function(event){
            var element = $(event.target);
            var id = parent.attr('id').split('-');
            var self = this;
            $.post("/json/vote/comment/" + id[2], "vote=up").done(function(data) {
                //self.comments.push(data.comment);
                //self._newComment(data.comment.id, data.comment.parent, "username", data.comment.text);
                //event.data.remove();
                upvote.addClass("uservote");
                if(downvote.hasClass("uservote")) downvote.removeClass("uservote");
                vote.empty().append(data.comment.vote + " points");
                upvote.unbind("click");
                downvote.bind("click", downvotefunc);
            }).fail(function(){
                alert("An error occured. You must be logged in to use this function.");
            });
        }, this);

        if(comment.uservote >= 0) {
            if(comment.attributes.uservote > 0) upvote.addClass('uservote');
            downvote.bind("click", downvotefunc);
        }

        if(comment.uservote <= 0) {
            if(comment.attributes.uservote < 0) downvote.addClass('uservote');
            upvote.bind("click", upvotefunc);
        }

        var pull = $('<div></div>').addClass('comment-nav');
        pull.append(upvote);
        pull.append(downvote);
        pull.append(reply_button);

        var top = $('<div id="comment-id-' + comment.id + '"></div>').addClass("panel panel-comment" + " parent-id-" + comment.attributes.parent);
        var heading = $('<div data-toggle="collapse" href="#' + comment.id + '" aria-expanded="true" aria-controls="' + comment.id + '"></div>').addClass("panel-heading");
        var title = $('<span></span>').addClass("panel-title").append(comment.user).append(vote);
        //heading.append(pull);

        heading.append(title);
        top.append(heading);

        var body = $('<div id="' + comment.id + '"></div>').addClass("panel-body collapse").append(comment.attributes.text);
        body.append(pull);

        var container = parent;

        top.append(body);
        container.append(top);

        if(comment.attributes.children && comment.attributes.children.length > 0) {
            $.each(comment.attributes.children, function(index, value){
                newNodeComment( value, body );
            });
        }
    }
};