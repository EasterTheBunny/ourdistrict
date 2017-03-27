/*
* Copyright (C) 2017
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

(function( $ ) {
	$.widget( "cp.topictree" , {
		
		// default options
		options: {
			depth: 20,
			data: [],
			size: { w: 50, h: 50}
		},
		
		_create: function() {
			var self = this;
			this.topic = this.element.attr('topic');

			$('body').on('contextmenu', '#frame-outer', function(e){ return false; });

			$.ajax({
				url: "/json/topic/" + this.topic,
				dataType: "json"
			}).done(function(json){
				self.data = json;
				self._attach();
			});
			
			return this;
		},
		
		_attach: function() {
			// plugin logic, options are already merged in this.options
			this.element.addClass( "topictree" );
			this.width = this.element.width();
			this.height = $(window).height() - 50;

			this.stage = new Konva.Stage({
				container: this.element.attr('id'),
				width: this.element.width(),
				height: $(window).height() - 50
			});
			
			this.midpoint = { x: this.width / 2, y: this.height / 2 };
			this.itemSize = this.options.size;
			
			this.sortOption = "vote";
			$('#sort-date').on('click', $.proxy(function(){
				this.sortOption = "date";
				$('#sort-indicator').html('sections sorted by date');
				this._redraw();
				this._setNode(this.currentNode);
			}, this));
			$('#sort-vote').on('click', $.proxy(function(){
				this.sortOption = "vote";
				$('#sort-indicator').html('sections sorted by vote');
				this._redraw();
				this._setNode(this.currentNode);
			}, this));
			$('#sort-indicator').html('sections sorted by vote');
		
			this.layer = new Konva.Layer();
			this.linesLayer = new Konva.Layer();
			this.tipsLayer = new Konva.Layer();
			this.lineageLayer = new Konva.Layer();
			this.dragLayer = new Konva.Layer();
			this._sort();
			this._addEvents();
			//this._getComments(this.tiers[0][0], this.items[this.tiers[0][0]][0].version, 1);
			
			this.currentNode = this.items[this.tiers[0][0]][0];
			this._setNode(this.currentNode);
			/*  
			var downvote = $('<span id="node-downvote" class="glyphicon glyphicon-thumbs-down" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="vote down">&nbsp;</span>');
			var upvote = $('<span id="node-upvote" class="glyphicon glyphicon-thumbs-up" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="vote up">&nbsp;</span>');
			var vote = $('<span id="node-interest-value" style="margin-right:15px;font-size:12px;"></span>').append(this.items[this.tiers[0][0]][0].vote);
			var pull = $('<div></div>');
			
			var downvotefunc = $.proxy(function(event){
				var element = $(event.target);
				var self = this;
				$.post(this.domain + "/json/vote/node/" + this.currentNode.id, "vote=down").done(function(data) {
					$("#node-interest-value").empty().append(data.node.vote);
					$("#node-upvote").toggleClass('uservote');
					$("#node-downvote").toggleClass('uservote');
					downvote.unbind("click");
					upvote.bind("click", upvotefunc);
				}).fail(function(){
					alert("An error occured. You must be logged in to use this function.");
				});
			}, this);
			
			var upvotefunc = $.proxy(function(event){
				var element = $(event.target);
				var self = this;
				$.post(this.domain + "/json/vote/node/" + this.currentNode.id, "vote=up").done(function(data) {
					$("#node-interest-value").empty().append(data.node.vote);
					$("#node-upvote").toggleClass('uservote');
					$("#node-downvote").toggleClass('uservote');
					upvote.unbind("click");
					downvote.bind("click", downvotefunc);
				}).fail(function(){
					alert("An error occured. You must be logged in to use this function.");
				});
			}, this);
			
			if(this.currentNode.uservote >= 0) {
				downvote.bind("click", downvotefunc);
				
				if(this.currentNode.uservote > 0) upvote.addClass('uservote');
			}
			
			if(this.currentNode.uservote <= 0) {
				upvote.bind("click", upvotefunc);
				
				if(this.currentNode.uservote < 0) downvote.addClass('uservote');
			}
			
			pull.append(vote);
			pull.append(upvote);
			pull.append(downvote);
			
			$('#node-details-title').empty();
			$('#node-details-title').append(this.items[this.tiers[0][0]][0].statement);
			$('#node-details-owner').empty();
			$('#node-details-owner').append(this.items[this.tiers[0][0]][0].user)
			$('#node-details-version').empty();
			$('#node-details-version').append(0);
			$('#node-details-text').empty();
			$('#node-details-text').append(this.items[this.tiers[0][0]][0].details);
			$('#node-details-interest').empty();
			$('#node-details-interest').append(pull);
				
			$('input[name="current-node-id"]').val(this.currentNode.id);
			$('input[name="current-topic-id"]').val(this.topic);
				
			//$("#new-comment-form :input[name='current-node-id']").val(node.getAttr('key'));
			$("#new-comment-form :input[name='current-node-version']").val(this.currentNode.version);
			$("#new-comment-form :input[name='current-comment-id']").val("root");
			$("#new-comment-form :input[name='new-comment-text']").val("");
				
			$('#current-node-id').val(this.currentNode.id);
			$('#version-node-id').value = this.currentNode.id;
			
			$('#comment-submit').on('click', $.proxy(function(event){this._submitNewComment();}, this));
			$('#version-submit').on('click', $.proxy(function(event){this._submitNewVersion();}, this));
			$('#child-submit').on('click', $.proxy(function(event){this._submitNewNode();}, this));*/
			
			$(window).resize($.proxy(function(){
				this._redraw();
			}, this));
		},
		
		_sort: function() {
			var root = "root";
			this.highvote = 0;
			this.lowvote = 10000000;
			var firstSort = {};
			var items = {};
			var tiers = [];
			
			var dataself = this;
	  
			$.each(this.data, function(index, item){
				if(firstSort[item.parent] != null){
					firstSort[item.parent].push(item);
				} else firstSort[item.parent] = [item];
		
				if(item.vote > dataself.highvote) dataself.highvote = item.vote;
				if(item.vote < dataself.lowvote) dataself.lowvote = item.vote;
				
				if(items[item.id] != null){
					items[item.id].push(item);
				} else items[item.id] = [item];
			});
	  
			//console.log(firstSort);
			//console.log(items);
			this.items = items;
			//console.log("----------");
			
			var workingSet = [firstSort[root][0].id];
			//console.log(workingSet);
			do{
				var nextSet = [];
				tiers.push(workingSet);
				$.each(workingSet, function(index, work){
					if(firstSort[work] != null){
						$.each(firstSort[work], function(key, x){
							if($.inArray(x.id, nextSet) == -1) nextSet.push(x.id);
						});
					}
				});
				workingSet = nextSet;
			}
			while(workingSet.length > 0);
			//tiers.sort(function(a, b){return a.length - b.length});
			//console.log(tiers);
	  
			var matrix = [];
			var workingTier = tiers.length - 1;						// start at the bottom tier
			while(workingTier >= 0) {								// interate to the top
				var nodes = tiers[workingTier];						// collect all nodes for tier
				var slf = this;
				$.each(nodes, function(index, node){				
					var parent = items[node][0].parent;				// get node's parent id
					var lineage = [node, parent];					// establish lineage array (ex: [5, 3, 1, 0])
					while(items[parent] != null) {					
						parent = items[parent][0].parent;
						lineage.push(parent);						
					}
		  
					var x = [];
					for(var y = lineage.length - 1; y >= 0; y--) {	// flop lineage (ex: [0, 1, 3, 5])
						x.push(lineage[y]);
					}
		  
					/*
					*	remove duplicates
					*	ex:
					*	0 0 0 0 0 0 0
					*	1 1 1 1 1 1 1
					*	3 3 2 2 3 4
					*	5 6
					*
					*	0 0 0 0
					*	1 1 1 1
					*	3 3 2 4
					*	5 6
					*
					*/
					if(!slf._discard(x, matrix)) matrix.push(x);
				});
				workingTier--;
			}
			//console.log("matrix: ");
			//console.log(matrix);
	  
			/*
			*	
			*/
			var sorted = matrix;
			for(var t = tiers.length; t >= 0; t--){				// iterate through tiers beginning at bottom
				//console.log("tier " + t);
				var objs = {n: []};								// 
				$.each(sorted, function(index, sortedElement){	// iterate through sorted
					var keys = Object.keys(objs);				// collect all keys from objs
					//console.log(objs);
					//console.log(keys);
					if(sortedElement[t] != null) {				// if element contains a value at current tier level
						var key = sortedElement[t].toString();	// create a key from previous value
						if($.inArray(key, keys) >= 0) {			
							//console.log(key);
							objs[key].push(sortedElement);		// if new key is in array of keys, include element under key
						}
						else objs[key] = [sortedElement];		// if new key is not in array of keys, add it and include element under key
					}else {
						objs.n.push(sortedElement);				// else include element to key 'n'
					}
				});
		
				var sorts = [];
				$.each(objs, function(key, value){
					$.merge(sorts, value);
				});
				sorted = sorts;
			}
			this.sorted = sorted;
			//console.log(sorted);
	  
			this.tiers = tiers;
			this.items = items;
			this.root = root;
			this.firstSort = firstSort;
	  
			this.tooltip = new Konva.Label({
				opacity: 0.75,
				visible: false,
				listening: false
			});

			this.tooltip.add(new Konva.Tag({
				fill: 'black',
				pointerDirection: 'down',
				pointerWidth: 10,
				pointerHeight: 10,
				lineJoin: 'round',
				shadowColor: 'black',
				shadowBlur: 10,
				shadowOffset: {x:5,y:5},
				shadowOpacity: 0.5
			}));
	  
				
			this.tooltip.add(new Konva.Text({
				text: '',
				fontFamily: 'Calibri',
				fontSize: 18,
				padding: 5,
				fill: 'white'
			}));
	  
			this.tipsLayer.add(this.tooltip);
	  
			this._drawTiers();
			this.stage.add(this.linesLayer);
			this.stage.add(this.layer);
			this.stage.add(this.lineageLayer);
			this.stage.add(this.tipsLayer);
			this.stage.add(this.dragLayer);
			
		},
		
		_discard: function(array, inarray) {
			var self = this;
			var hastwin = false;
			var haschild = false;
			$.each(inarray, function(index, arg) {
				if(array.length == arg.length) {			// if identical length, compare elements
					var match = true;
					$.each(array, function(ind, elem) {
						if(arg[ind] != elem) match = false;
					});
					if(!hastwin) hastwin = match;
				} else if(array.length < arg.length) {		// if testing array is shorter than existing array
					var match = true;
					$.each(array, function(ind, elem) {
						if(arg[ind] != elem) match = false;
					});
					if(!haschild) haschild = match;
				}
			});
			
			if(hastwin || haschild) return true;
			else return false;
		},
	
		_drawTiers: function() {
			var data = this.sorted;
			var tiers = this.tiers;
			//var drawnNodes = {};
			var parentPlacement = {};
			var parents = {};
			var xpad = 25;
			var ypad = 5;
			var swidth = 1;
			var scolor = '#a5a4b3';
			//console.log(data);
			//console.log(tiers);
	  
			var nodeVertical = this.height / tiers.length;		// vertical spacing
			var minSpace = (this.width - (2 * xpad)) / data.length;			// horizontal spacing
			//console.log(this.width);
			//console.log(data.length);
			//console.log(minSpace);
			/*
			*	optimize item size for available vertical space
			*/
			while(nodeVertical < (this.itemSize.h + 15)) {
				this.itemSize.h = this.itemSize.h - 1;
			}
			
			/*
			*	optimize item size for available horizontal space
			*/
			while(minSpace < this.itemSize.w + 20) {
				this.itemSize.w = this.itemSize.w - 1;
			}
	  
			/*
			*	square up the node dimensions
			*/
			if(this.itemSize.w > this.itemSize.h) {
				this.itemSize.w = this.itemSize.h;
			}
	  
			/*
			*	square up the node dimensions
			*/
			if(this.itemSize.h > this.itemSize.w) {
				this.itemSize.h = this.itemSize.w;
			}
	  
			var growth = (minSpace - this.itemSize.w) / data.length;
			//minSpace = minSpace - this.itemSize.w;
	  
			/*
			*	iterate through tiers from bottom up
			*
			*	0 0 0 0 1 0		<-
			*	1 0 0 1 0 0		<-
			*/
			for(var i = tiers.length; i > 0; i--) {
				var drawnNodes = [];															// nodes already drawn
				var count = xpad;															// left draw point (starts at left padding)
				parents = parentPlacement;													// array of parents for current tier in iteration
				parentPlacement = {};														// array of parents where placement is defined by left and right most points of all children; built for next iteration
		
				var self = this;															// maintain scope
		
				/*
				*	connecting lines are calculated for nodes with only one direct decendent first
				*	
				*/
				$.each(parents, function(key, value){
					
					if((value.right - value.left) <= 50) {					// <------------ why the value 50??????
						//console.log("key: " +  key + ", one line");
						var x = value.left + self.itemSize.w / 2;
						var ybottom = (i * nodeVertical) + (self.itemSize.h / 2) - 10;			// bottom point for vertical connecting lines for all nodes with only one decendent
						var ytop = ((i - 1 ) * nodeVertical) + (self.itemSize.h / 2) + 60;		// top point for vertical connecting lines for all nodes with only one decendent
			
						var line = new Konva.Line({
							points: [x, ybottom, x, ytop],
							tension: 1,
							stroke: scolor,
							strokeWidth: swidth
						});
						self.linesLayer.add(line);
					} else {
						//console.log("key: " +  key + ", four lines");
						var left = value.left + self.itemSize.w / 2;
						var right = value.right - self.itemSize.w / 2;
						var ybottom = (i * nodeVertical) + (self.itemSize.h / 2) - 10;			// bottom point for vertical connecting lines
						var ymiddle = ((i - 1 ) * nodeVertical) + (self.itemSize.h / 2) + 80;	// vertical midpoint between tiers
						var ytop = ((i - 1 ) * nodeVertical) + (self.itemSize.h / 2) + 60;		// top point from vertical connecting lines
						var midpoint = ((right - left) / 2) + left;								// horizontal midpoint for midpoint connecting lines
			
						// horizontal connecting lines
						var line = new Konva.Line({
							points: [left, ybottom, left, ymiddle, right, ymiddle, right, ybottom],
							tension: 0,
							stroke: scolor,
							strokeWidth: swidth
						});
						self.linesLayer.add(line);
			
						// midpoint vertical connecting lines
						var line = new Konva.Line({
							points: [midpoint, ymiddle, midpoint, ytop],
							tension: 0,
							stroke: scolor,
							strokeWidth: swidth
						});
						self.linesLayer.add(line);
					}
				});
		
				/*
				*	iterate through all nodes at current tier index (includes 'phantom' nodes)
				*	
				*	| | | | | |
				*	v v v v v v
				*
				*	0 0 0 0 1 0
				*	1 0 0 1 0 0
				*/
				$.each(data, function(index, array){
					
					/*
					*	if array length is 1 or more greater than i (current tier index), draw a node
					*/
					if(array.length - 1 >= i) {																		// <------ could this be (array.length > i) ????????
		  
						// get the node to be drawn (element from current data array at current tier index)
						var node = array[i];
			  
						// draw node if it hasn't been
						if($.inArray(node, drawnNodes) == -1) {
			  
							// x position of new node
							var xstat = count;
			  
							// y position of new node
							var ystat = ((i -1 ) * nodeVertical) + (self.itemSize.h / 2);
			  
			  
							if(parents[node.toString()] != undefined) {
								var parent = parents[node.toString()];
								xstat = (((parent.right - parent.left) / 2) + parent.left) - (self.itemSize.w / 2);
								//count = parent.right;
							}
			  
							//console.log("node: " + node + " (" + xstat + "," + ystat + "), parent: " + array[i-1]);
			  
							var pad = 0;
							var slf = self;
							
							/* 
							 *	sort versions
							 *
							var sortedByOptions = [];
							$.each(self.items[node.toString()], function(key, value){
								if(sortedByOptions.length == 0) sortedByOptions.push(value);
								else {
									if(value.uservote > 0) sortedByOptions.unshift(value);
									else {
										sortedByOptions.push(value);
									}
								}
							});*/
							
							var lin = [];
							if(i - 1 >= 0) lin = array.slice(0, i+1);
							
							if(self.sortOption == "vote") {
								self.items[node.toString()].sort(function(a, b) {
									if(a.vote < b.vote) return -1;
									if(a.vote > b.vote) return 1;
									return 0;
								});
							} else if(self.sortOption == "date") {
								self.items[node.toString()].sort(function(a, b) {
									return new Date(a.date) - new Date(b.date);
								});
							}
							/*
							*	draw every version with a tool tip for each node position
							*/
							//console.log("----new node group----");
							$.each(self.items[node.toString()], function(key, value){
								value.key = key;
								
								// top - #00ff00
								// middle - #ffff00
								// bottom - #ff0000
								var red = 0;
								var green = 100;
								var blue = 15;
								if((slf.highvote - slf.lowvote) > 0) {
									var vote = Math.floor((value.vote - slf.lowvote)/(slf.highvote - slf.lowvote)*100);
									//console.log(vote);
									if(vote >= 50) {
										green = 100;
										red = vote;
									} else {
										red = 100;
										green = vote;
									}
								}
								//console.log(red + " " + green + " " + blue);
								var color = "rgba(" + red + "%," + green + "%," + blue + "%,0.8)";
								//console.log(color);
								color = "#DDD";
								
								var child = new Konva.Rect({
									x: xstat + pad,
									y: ystat,
									stroke: '#666',
									strokeWidth: 1,
									fill: color,
									width: slf.itemSize.w,
									height: slf.itemSize.h,
									cornerRadius: 5,
									draggable: false,
									shadowColor: 'black',
									shadowBlur: 0,
									shadowOffset: {
										x : 0,
										y : 0
									},
									key: value.id,
									version: value.key
								});
								
								slf.items[value.id][value.key].node = child;
								slf.items[value.id][value.key].location = {x: xstat + pad, y: ystat};
								slf.items[value.id][value.key].lineage = lin;
								
								//console.log(value.id + " " + value.version + " " + value.vote + " x:" + (xstat + pad) + " y:" + ystat);
								
								slf.layer.add(child);
								pad += 5;
							});
			  
							count = xstat;
			  
							/*
							*	calculate parent coordinates and create it in parentPlacement array if it doesn't exist
							*	set left limit for parent at first node
							*	set right limit for parent at final node
							*/
							if(parentPlacement[(self.items[node][0]["parent"]).toString()] == undefined){
								parentPlacement[(self.items[node][0]["parent"]).toString()] = {left: count, right: count + self.itemSize.w};
								//console.log("left: " + count + ", right: " + (count + this.itemSize.w));
							} else {
								parentPlacement[(self.items[node][0]["parent"]).toString()]["right"] = count + self.itemSize.w;
								//console.log("node: " + array[i-1] + ", right: " + (count+this.itemSize.w));
							}
			
			  
							//count += self.itemSize.w + minSpace - xpad;
							count += minSpace + growth;
							//console.log("tier: " + i + " count: " + count);
			
						}
						drawnNodes.push(node);									// add node to drawn nodes array
					}else {
					
						/*
						*	if array length is equal to i (current tier index), draw phantom node
						*/
						if(array.length == i) {
							//console.log("phantom parent: " + array[i-1]);
				
							/*
							*	calculate parent coordinates and create it in parentPlacement array if it doesn't exist
							*/
							if(parentPlacement[(array[i-1]).toString()] == undefined){
								parentPlacement[(array[i-1]).toString()] = {left: count, right: count + self.itemSize.w};
							} else {
								parentPlacement[(array[i-1]).toString()]["right"] = count + self.itemSize.w;
							}
			  
							var xstat = count;												// set x coordinate for node
							var ystat = ((i - 1 ) * nodeVertical) + (self.itemSize.h / 2);	// set y coordinate for node
				
							var child = new Konva.Rect({	  	      						// draw node
								x: xstat,
								y: ystat,
								stroke: '#CCC',
								strokeWidth: 1,
								fill: '#fff',
								width: self.itemSize.w,
								height: self.itemSize.h,
								cornerRadius: 5
							});
							self.layer.add(child);
							/*
							var line = new Konva.Line({
								points: [xstat + this.itemSize.w / 2, ystat - 10, xstat + this.itemSize.w / 2, ((i - 2 ) * nodeVertical) + (this.itemSize.h / 2) + 60],
								tension: 1,
								stroke: 'red',
								strokeWidth: 2
							});
							this.layer.add(line);*/
				
							//console.log("draw phantom (" + count + ",y)");
							//count += self.itemSize.w + minSpace - xpad;
							count += minSpace + growth;
							//console.log("tier: " + i + " count: " + count + " (phantom drawn)");
						} else {
							//count += self.itemSize.w + minSpace - xpad;
							count += minSpace + growth;
							//console.log("tier: " + i + " count: " + count + " (phantom)");
						}
					}
				});
				//console.log(drawnNodes);
				//console.log(parentPlacement);
			}
			//console.log(this.items);
		},
		
		_redraw: function() {
			
			$.each(this.items, $.proxy(function(key, item){
				$.each(item, $.proxy(function(index, value){
					value.node = null;
				}, this));
			}, this));
			
			this.layer.destroyChildren();
			this.linesLayer.destroyChildren();
			this.tipsLayer.destroyChildren();
			this.lineageLayer.destroyChildren();
			
			this._sort();
			
		},
		
		_getComments: function(nodeid, version, topic) {
			var self = this;
			$.ajax({
				url: "/json/comments/" + this.topic + "/" + nodeid + "/" + version,
				dataType: "json"
			}).done(function(json){
				self.comments = json;
				self._drawComments();
			});
		},
		
		_drawComments: function() {
			var container = $("#comment-id-root");
			container.empty();
			var self = this;
			$.each(this.comments, function(index, value) {
				
				self._newComment(value);
				
				$('[data-toggle="tooltip"]').tooltip()
			});
			$('.collapse.panel-body').collapse('show');
		},
		
		_newComment: function(comment) {
			// if parent exists in DOM, insert into parent
			// if children exist in the DOM, wrap them into element
			
			var reply_button = $('<span class="glyphicon glyphicon-comment" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="reply">&nbsp;</span>');
			var downvote = $('<span class="glyphicon glyphicon-thumbs-down" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="vote down">&nbsp;</span>');
			var upvote = $('<span class="glyphicon glyphicon-thumbs-up" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="vote up">&nbsp;</span>');
			var vote = $('<span class="pull-right" style="margin-right:15px;font-size:12px;"></span>').append(comment.vote + " points");
			//vote.append(vote);
			
			reply_button.bind("click", $.proxy(this._newCommentReply, this));
			
			var downvotefunc = $.proxy(function(event){
				var element = $(event.target);
				var parent = element.parent().parent().parent();
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
				var parent = element.parent().parent().parent();
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
				if(comment.uservote > 0) upvote.addClass('uservote');
				downvote.bind("click", downvotefunc);
			}
			
			if(comment.uservote <= 0) {
				if(comment.uservote < 0) downvote.addClass('uservote');
				upvote.bind("click", upvotefunc);
			}
			
			var pull = $('<div></div>').addClass('comment-nav');
			pull.append(upvote);
			pull.append(downvote);
			pull.append(reply_button);
			
			var top = $('<div id="comment-id-' + comment.id + '"></div>').addClass("panel panel-comment" + " parent-id-" + comment.parent);
			var heading = $('<div data-toggle="collapse" href="#' + comment.id + '" aria-expanded="true" aria-controls="' + comment.id + '"></div>').addClass("panel-heading");
			var title = $('<span></span>').addClass("panel-title").append(comment.user).append(vote);
			//heading.append(pull);
				
			heading.append(title);
			top.append(heading);
				
			var body = $('<div id="' + comment.id + '"></div>').addClass("panel-body collapse").append(comment.text);
			body.append(pull);
			
			var container = "";
			
			if($(".parent-id-" + comment.id).length > 0) body.append($(".parent-id-" + comment.id));
			if($("#comment-id-" + comment.parent).length > 0) {
				container = $("#comment-id-" + comment.parent);
				var cnt = container.children('.panel-body');
				if(cnt.length >= 1) container = cnt;
			}
			else container = $("#comment-id-root");
				
			top.append(body);
			container.append(top);
		},
	
		_mouseover: function(evt) {
			var shape = evt.target;
			if (shape) {
				//if(shape.getAttr('key')) console.log(shape.getAttr('key'));
				shape.setOpacity(0.75);
				if(shape.getAttr('key')) {
					//console.log(shape.getAttr('key') + "  " + shape.getAttr('version'));
					var str = this.items[shape.getAttr('key')][shape.getAttr('version')].statement;
					str = str + '\n';
					str = str + "version: " + (shape.getAttr('version') + 1);
					str = str + '\n';
					str = str + "id: " + shape.getAttr('key');
					
					str = "Click for more details.";
					
					this._updateToolTip(str, shape.position().x + (this.itemSize.w / 2), shape.position().y);
					this.tipsLayer.batchDraw();
				}
				this.layer.draw();
			}
		},
	
		_mouseout: function(evt) {
			var shape = evt.target;
			if(shape) {
				shape.opacity(1);
				this.tooltip.hide();
				this.tipsLayer.draw();
				this.layer.draw();
			}
		},
	
		_mouseclick: function(evt) {
			var node = evt.target;
			console.log(evt);
			if(node) this._setNode(this.items[node.getAttr('key')][node.getAttr('version')]);
			
			/*
			if(this.currentTarget) {
				this.currentTarget.fill("#DDD");
			}
			if(node){
				this.currentTarget = node;
				node.fill("#333");
			}
			if(node.getAttr('key') != null) {
				this.currentNode = this.items[node.getAttr('key')][node.getAttr('version')];
				//console.log(node.getAttr('key'));
				//$('#nodeModal').modal('show');
				$('#current-node-id').val(node.getAttr('key'));
				$('#version-node-id').value = node.getAttr('key');
				
				$('#node-details-title').empty();
				$('#node-details-title').append(this.items[node.getAttr('key')][node.getAttr('version')].statement);
				$('#node-details-owner').empty();
				$('#node-details-owner').append(this.items[this.tiers[0][0]][0].user)
				$('#node-details-version').empty();
				$('#node-details-version').append(node.getAttr('version'));
				$('#node-details-text').empty();
				$('#node-details-text').append(this.items[node.getAttr('key')][node.getAttr('version')].details);
				
				//$("#new-version-form :input[name='current-node-id']").val(node.getAttr('key'));
				
				$('input[name="current-node-id"]').val(node.getAttr('key'));
				$('input[name="current-topic-id"]').val(this.topic);
				
				//$("#new-comment-form :input[name='current-node-id']").val(node.getAttr('key'));
				$("#new-comment-form :input[name='current-node-version']").val(node.getAttr('version'));
				$("#new-comment-form :input[name='current-comment-id']").val("root");
				$("#new-comment-form :input[name='new-comment-text']").val("");
				
				this._getComments(node.getAttr('key'), node.getAttr('version'), this.topic);
			}
			//console.log(node.getAttr('key'));
			//this.dataTray.setStyle('visibility','visible');
			//this.loadDataTray(node.getAttr('key'));
			//var layer = node.getLayer();
		
			//node.moveTo(this.lineageLayer);
			//layer.draw();
			//node.startDrag();
			this.layer.draw();*/
		},
		
		_dragstart: function(event) {
			var shape = event.target;
			shape.moveTo(this.dragLayer);
			this.stage.draw();
			
			this.reposition = {x: shape.getAttr('x'), y: shape.getAttr('y')};
			//console.log(this.reposition);
			
			//if(tween) {
			//	tween.pause();
			//}
			shape.setAttrs({
				shadowOffset: {
					x: 5,
					y: 5
				}
			});
		},
		
		_dragend: function(event) {
			var shape = event.target;
			shape.moveTo(this.layer);
			this.stage.draw();
			//console.log(this.reposition);
			var rep = this.reposition;
			
			  /* tween = new Konva.Tween({
				node: shape,
				duration: 1.0,
				easing: Konva.Easings.ElasticEaseOut,
				scaleX: shape.getAttr('startScale'),
				scaleY: shape.getAttr('startScale'),
				shadowOffsetX: 5,
				shadowOffsetY: 5,
							x: rep.x,
							y: rep.y
			  });

			  tween.play();*/
  
			
			shape.setAttrs({
				x: this.reposition.x,
				y: this.reposition.y,
				shadowOffset: {
					x: 0,
					y: 0
				}
			});
			this.layer.draw();
		},
		
		_setNode: function(target) {
			if(this.currentTarget) {
				this.currentTarget.fill("#DDD");
			}
			this.currentTarget = target.node;
			target.node.fill("#333");
			var loc = '';
			$.each(target.lineage, $.proxy(function(index, value){
				if(value != "root") {
					if(loc.length > 0) loc = loc + ";";
					loc = loc + value + ":" + this.items[value][0].version;
					//console.log(loc);
				}
			}, this))
			this.currentNode = target;
			
			
			var downvote = $('<span id="node-downvote" class="glyphicon glyphicon-thumbs-down" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="vote down">&nbsp;</span>');
			var upvote = $('<span id="node-upvote" class="glyphicon glyphicon-thumbs-up" aria-hidden="true" style="cursor:pointer" data-toggle="tooltip" data-placement="right" title="vote up">&nbsp;</span>');
			var vote = $('<span id="node-interest-value" style="margin-right:15px;font-size:12px;"></span>').append(target.vote);
			var pull = $('<div></div>');
			
			var downvotefunc = $.proxy(function(event){
				var element = $(event.target);
				var self = this;
				$.post(this.domain + "/json/vote/node/" + this.currentNode.id, "vote=down").done(function(data) {
					$("#node-interest-value").empty().append(data.node.vote);
					$("#node-upvote").toggleClass('uservote');
					$("#node-downvote").toggleClass('uservote');
					downvote.unbind("click");
					upvote.bind("click", upvotefunc);
				}).fail(function(){
					alert("An error occured. You must be logged in to use this function.");
				});
			}, this);
			
			var upvotefunc = $.proxy(function(event){
				var element = $(event.target);
				var self = this;
				$.post(this.domain + "/json/vote/node/" + this.currentNode.id, "vote=up").done(function(data) {
					$("#node-interest-value").empty().append(data.node.vote);
					$("#node-upvote").toggleClass('uservote');
					$("#node-downvote").toggleClass('uservote');
					upvote.unbind("click");
					downvote.bind("click", downvotefunc);
				}).fail(function(){
					alert("An error occured. You must be logged in to use this function.");
				});
			}, this);
			
			if(this.currentNode.uservote >= 0) {
				downvote.bind("click", downvotefunc);
				
				if(this.currentNode.uservote > 0) upvote.addClass('uservote');
			}
			
			if(this.currentNode.uservote <= 0) {
				upvote.bind("click", upvotefunc);
				
				if(this.currentNode.uservote < 0) downvote.addClass('uservote');
			}
			
			pull.append(vote);
			pull.append(upvote);
			pull.append(downvote);
			
			
			
			
			$('#pdf-section').attr('href', '/pdf/node/' + target.id + ".pdf");
			$('#pdf-comments').attr('href', '/pdf/comments/' + target.id + ".pdf");
			$('#pdf-document').attr('href', '/pdf/topic/' + this.topic + '.pdf?node-list=' + loc);
			
			$('#current-node-id').val(target.id);
			$('#version-node-id').value = target.id;
				
			$('#node-details-title').empty();
			$('#node-details-title').append(target.statement);
			$('#node-details-owner').empty();
			$('#node-details-owner').append(target.user)
			$('#node-details-version').empty();
			$('#node-details-version').append(target.version);
			$('#node-details-text').empty();
			$('#node-details-text').append(target.details);
			$('#node-details-interest').empty();
			$('#node-details-interest').append(pull);
				
			//$("#new-version-form :input[name='current-node-id']").val(node.getAttr('key'));
				
			$('input[name="current-node-id"]').val(target.id);
			$('input[name="current-topic-id"]').val(this.topic);
				
			//$("#new-comment-form :input[name='current-node-id']").val(node.getAttr('key'));
			$("#new-comment-form :input[name='current-node-version']").val(target.version);
			$("#new-comment-form :input[name='current-comment-id']").val("root");
			$("#new-comment-form :input[name='new-comment-text']").val("");
				
			this._getComments(target.id, target.version, this.topic);
			
			this.lineageLayer.removeChildren();
			
			$.each(target.lineage, $.proxy(function(index, value) {
				if(index - 1 > 0) {
					var parentLoc = this.items[target.lineage[index-1]][0].location;
					var loc = this.items[value][0].location;
					var xmid = this.itemSize.w / 2;
					var ymid = this.itemSize.h / 2;
					
					var line = new Konva.Line({
						points: [loc.x + xmid, loc.y + ymid, parentLoc.x + xmid, parentLoc.y + ymid],
						tension: 0,
						stroke: "#000",
						strokeWidth: 1
					});
				
					this.lineageLayer.add(line);
				}
			}, this));
			
			
			this.lineageLayer.batchDraw();
			
			this.layer.batchDraw();
		},
	
		_updateToolTip: function(text, x, y) {
			this.tooltip.getText().text(text);
			this.tooltip.position({x:x, y:y});
			this.tooltip.show();
		},
	
		_addEvents: function() {
			var stage = this.stage;
			stage.on('mouseover', $.proxy(this._mouseover, this));
			stage.on('mouseout', $.proxy(this._mouseout, this));
			stage.on('click', $.proxy(this._mouseclick, this));
			stage.on('dragstart', $.proxy(this._dragstart, this));
			stage.on('dragend', $.proxy(this._dragend, this));
		},
		
		_newCommentReply: function(event) {
			var element = $(event.target);
			var parent = element.parent().parent().parent();
			var body = parent.children('.panel-body');
			
			var form = $("<form></form>");
			var group1 = $('<div class="form-group"></div>');
			var input = $('<textarea class="form-control" rows="3" name="new-comment-text" placeholder="comment"></textarea>');
			var id = parent.attr('id').split('-');
			var hidden1 = $('<input type="hidden" value="" name="current-comment-id">').val(id[2]);
			console.log(this.topic);
			var hidden2 = $('<input type="hidden" value="" name="current-topic-id">').val(this.topic);
			var hidden3 = $('<input type="hidden" value="" name="current-node-version">').val(this.currentNode.version);
			var hidden4 = $('<input type="hidden" value="" name="current-node-id">').val(this.currentNode.id);
			var group2 = $('<div class="form-group"></div>');
			var cancel = $('<button type="button" class="btn btn-default">Cancel</button>');
			var submit = $('<button type="button" class="btn btn-primary pull-right">Reply</button>');
			
			form.append(hidden1);
			form.append(hidden2);
			form.append(hidden3);
			form.append(hidden4);
			
			group1.append(input);
			form.append(group1);
			
			
			group2.append(cancel);
			group2.append(submit);
			form.append(group2);
			
			form.prependTo(body);
			
			cancel.click(form, function(event){event.data.remove();});
			submit.click(form, $.proxy(function(event){
				var self = this;
				$.post("/json/add/comment", event.data.serialize()).done(function(data) {
					self.comments.push(data.comment);
					self._newComment(data.comment);
					event.data.remove();
				}).fail(function(){
					event.data.remove();
					alert("An error occured. You must be logged in to use this function.");
				});
			}, this));
		},
		
		submitNewNode: function() {
			var self = this;
			$.post("/json/add/node", $('#new-node-form').serialize()).done(function(data) {
				self.data.push(data.node);
				console.log(self.items);
				console.log(data.node.id);
				self.currentNode = self.items[data.node.id][0];
				self._redraw();
				$('#section-info li:eq(0) a').tab('show');
				$("#new-comment-form :input[name='new-node-statement']").val('');
				$("#new-comment-form :input[name='new-node-details']").val('');
				//$('#nodeModal').modal('hide');
			}).fail(function(){
				alert("An error occured. You must be logged in to use this function.");
			});
		},
		
		submitNewVersion: function() {
			var self = this;
			$.post("/json/add/version", $('#new-version-form').serialize()).done(function(data) {
				self.data.push(data.node);
				self.currentNode = self.items[data.node.id][data.node.version];
				self._redraw();
				$('#section-info li:eq(0) a').tab('show');
				//$('#nodeModal').modal('hide');
			}).fail(function(){
						alert("An error occured. You must be logged in to use this function.");
					});
		},
		
		submitNewComment: function() {
			var self = this;
			$.post("/json/add/comment", $('#new-comment-form').serialize()).done(function(data) {
				self.comments.push(data.comment);
				self._getComments($("#new-comment-form :input[name='current-node-id']").val(), $("#new-comment-form :input[name='current-node-version']").val(), 1);
				$('#section-info li:eq(1) a').tab('show');
				$("#new-comment-form :input[name='new-comment-text']").val('');
			}).fail(function(){
                alert("An error occured. You must be logged in to use this function.");
            });
		}
		
	});
})( jQuery );