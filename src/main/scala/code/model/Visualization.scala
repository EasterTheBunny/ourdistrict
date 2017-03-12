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

package code
package model

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util._
import net.liftweb.http._
import net.liftweb.sitemap._
import Helpers._
import Loc._
import js._
import JsCmds._
import JE._

import scala.xml.{NodeSeq,Elem}
import scala.xml.transform._

class Visualization extends LongKeyedMapper[Visualization] with IdPK {
  def getSingleton = Visualization
  object owner extends MappedLongForeignKey(this, User)
  object title extends MappedString(this, 25)
  object short_desc extends MappedString(this, 140)
  object script extends MappedString(this, 50000)
  object date_created extends MappedDateTime(this)
  object last_updated extends MappedDateTime(this)
}

object Visualization extends Visualization with LongKeyedMetaMapper[Visualization] {
  override def dbTableName = "visualization"

  def screenWrap = Full(<div id="main" class="lift:surround?with=minimal;at=content"><div class="container-fluid"><div class="row"><lift:bind /></div></div><script src="http://d3js.org/d3.v3.js"></script>{ Script(cursorPosition) }</div>)
  
  lazy val testLoggedIn = If(User.loggedIn_? _, S.?("must.be.logged.in"))
  
  val addVisualizationPath = "visualization" :: Nil
    
  def addVisualizationMenuLoc: Box[Menu] =
    Full(Menu(Loc("Add Visualization", addVisualizationPath, "Data Forge", addVisualizationMenuLocParams)))
    
  protected def addVisualizationMenuLocParams: List[LocParam[Unit]] = 
    Template(() => wrapIt(addVisualization)) ::
    testLoggedIn ::
    LocGroup("data") ::
    Nil
    
  final case object AddVisualizationMenusAfter extends Loc.LocParam[Any]
  final case object AddVisualizationMenusHere extends Loc.LocParam[Any]
  final case object AddVisualizationMenusUnder extends Loc.LocParam[Any]
  
  private lazy val AfterUnapply = SiteMap.buildMenuMatcher(_ == AddVisualizationMenusAfter)
  private lazy val HereUnapply = SiteMap.buildMenuMatcher(_ == AddVisualizationMenusHere)
  private lazy val UnderUnapply = SiteMap.buildMenuMatcher(_ == AddVisualizationMenusUnder)
  
  def sitemapMutator: SiteMap => SiteMap = SiteMap.sitemapMutator {
    case AfterUnapply(menu) => menu :: sitemap
    case HereUnapply(_) => sitemap
    case UnderUnapply(menu) => List(menu.rebuild(_ ::: sitemap))
  }(SiteMap.addMenusAtEndMutator(sitemap))
  
  lazy val sitemap: List[Menu] = List(addVisualizationMenuLoc).flatten(a => a)
  
  def addVisualization = {
    var visualizationTitle = ""
    var visualizationDesc = ""
    var visualizationScript = ""
    
    def testAndSet() {
      val user = User.currentUserLog
      
      if(true){
        val newVisualization = Visualization.create
        newVisualization.title(visualizationTitle).short_desc(visualizationDesc).script(visualizationScript).date_created(new java.util.Date()).last_updated(new java.util.Date()).owner(user)
        
        newVisualization.validate match {
          case Nil => {
             newVisualization.save
             
             S.notice("new visualization created")
             S.redirectTo("/charts")
          }
          case xs => S.error(xs)
        }
      }
    }
    
    val bind = 	"@title" #> SHtml.text("", s => visualizationTitle = s.trim) &
    			"@desc" #> SHtml.text("", s => visualizationDesc = s.trim) &
    			"@script" #> SHtml.textarea("", s => visualizationScript = s.trim) &
    			"@pie" #> SHtml.ajaxButton(<span class="glyphicon glyphicon-adjust" aria-hidden="true"></span>, func _, ("aria-label" -> "Left Align"), ("class" -> "btn btn-info"), ("data-toggle" -> "tooltip"), ("data-placement" -> "right"), ("title" -> "Basic Pie Chart")) &
    			"type=submit" #> SHtml.submit(S.?("Save"), testAndSet _)
    			
    bind(addTopicXhtml)
  }
  
  private def func = Run("""
		  $('#visualization-script').insertAtCursor('
d3.json("/static/json/senate-party-lines.json", function(error, root) {
~    var width = 960,
~        height = 700,radius = Math.min(width, height) / 2
~
~    var color3 = d3.scale.ordinal()
~        .domain(["republican","democrat"])
~        .range(["red","blue","yellow"]);
~~    var vis = d3.select("#graph-container").append("svg:svg").data([root]).attr("width", width).attr("height", height).append("svg:g").attr("transform", "translate(" + width / 2 + "," + height * .52 + ")");
~~    var pie = d3.layout.pie().sort(null).value(function(d){ return d.count; });
~~    var arc = d3.svg.arc()
~        .innerRadius(function(d) { return 100; })
~        .outerRadius(function(d) { return 250; })
~        .padAngle(function(d){ return .0123;});
~~    var arcs = vis.selectAll("g.slice").data(pie).enter().append("svg:g").attr("class","slice");
~~    arcs.append("svg:path")
~        .attr("fill", function(d) { return color3(d.data.name); })
~        .attr("d", function(d){ return arc(d); });
~});');
	  """.replaceAll("\n","").replaceAll("""~{1}""", """\\n"""))
  
  private lazy val newVisualizationInstructions = "Use D3.js or svg to create a nice visualization for your data."
 
  def addTopicXhtml = {
    (<form method="post" action={S.uri}>
    	<div class="col-md-3">
    	<table class="table table-striped"><thead><tr><td><h3>Create</h3></td></tr></thead>
    		<tbody>
    			<tr><td><h4>Data Visualization</h4></td></tr>
    			<tr><td>
    				<div class="form-group">
    					<label for="visualization-title">Title</label>
    					<input id="visualization-title" type="text" name="title" class="form-control" maxlength="25" placeholder="A Sexy Title" />
    				</div>
    			</td></tr>
    			<tr><td>
    				<div class="form-group">
    					<label for="visualization-desc">Short Description</label>
    					<input id="visualization-desc" type="text" name="desc" class="form-control" maxlength="140" placeholder="Short Description" />
    				</div>
    			</td></tr>
    			<tr><td>
    				<button type="button" name="pie" class="btn btn-primary" aria-label="Left Align">
    					<span class="glyphicon glyphicon-adjust" aria-hidden="true"></span>
    				</button>
    			</td></tr>
    			<tr><td><input type="submit" class="btn btn-primary pull-right" /></td></tr>
    		</tbody>
    	</table>
    	</div>
    	<div class="col-md-9">
    	  <div role="tabpanel" style="margin-top:32px">
    		<ul class="nav nav-tabs" role="tablist">
    		  <li role="presentation" class="active"><a href="#script" aria-controls="script" role="tab" data-toggle="tab"><span class="glyphicon glyphicon-pencil" aria-hidden="true"></span></a></li>
    		  <li role="presentation"><a id="test-script" href="#test" aria-controls="test" role="tab" data-toggle="tab"><span class="glyphicon glyphicon-ok" aria-hidden="true"></span></a></li>
    		</ul>
    		<div class="tab-content">
    		  <div role="tabpanel" class="tab-pane active" id="script" style="margin-top:10px">
    			
    			  <textarea id="visualization-script" name="script" class="form-control" rows="25" maxlength="50000" placeholder={ newVisualizationInstructions } />
    			
    		  </div>
    		  <div role="tabpanel" class="tab-pane" id="test"><div id="graph-container"></div></div>
    		</div>
    	  
    	  </div>
    	</div></form>)
  }
  
  private val cursorPosition = JsRaw("""(function ($, undefined) {
      $('#test-script').on('click', function(event){
		  $("#graph-container").empty();
		  eval($('#visualization-script').get(0).value);
	  });
      
    $.fn.getCursorPosition = function() {
        var el = $(this).get(0);
        var pos = 0;
        if('selectionStart' in el) {
            pos = el.selectionStart;
        } else if('selection' in document) {
            el.focus();
            var Sel = document.selection.createRange();
            var SelLength = document.selection.createRange().text.length;
            Sel.moveStart('character', -el.value.length);
            pos = Sel.text.length - SelLength;
        }
        return pos;
    }
      $(function () {
  $('[data-toggle="tooltip"]').tooltip()
})
      $.fn.insertAtCursor = function(text) {
		  var elem = $(this).get(0);
		  if(document.selection) {
		  	elem.focus();
		  	var select = document.selection.createRange();
		  	select.text = text;
		  }
      
		  else if(elem.selectionStart || elem.selectionState == '0') {
		  	var startpos = elem.selectionStart;
		  	var endpos = elem.selectionEnd;
		  	elem.value = elem.value.substring(0, startpos) + text + elem.value.substring(endpos, elem.value.length);
		  }
      
		  else {
		  	elem.value += text;
		  }
	  }
})(jQuery);""")
    
  protected def wrapIt(in: NodeSeq): NodeSeq =
    screenWrap.map(new RuleTransformer(new RewriteRule {
      override def transform(n: scala.xml.Node) = n match {
        case e: Elem if "bind" == e.label && "lift" == e.prefix => in
        case _ => n
      }
    })) openOr in
}