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
package snippet

import net.liftweb.mapper._
import net.liftweb._
import common._
import util._
import Helpers._
import http._
import sitemap._
import Loc._
import js._
import JsCmds._
import JE._

import scala.xml.Text

import code.model.Visualization

object ChartsPage {
  // define the menu item for the page that
  // will display all items
  def menu = Menu(Loc("Charts", "charts" :: Nil, "Charts 'n Stuff", LocGroup("site", "navbar")))
}

class ChartsPage() extends StatefulSnippet {
    
  private val vis = Visualization.findAll(OrderBy(Visualization.date_created, Descending))
  private var selected = vis.head
  
  def applyVisualization(in: Visualization) = {
    selected = in
    Run("""$('#graph-container').empty();""" + selected.script.get)
  }
  
  def dispatch = {
    case "render" => {
      "#graph-select" #> SHtml.ajaxSelectObj(vis.map(g => (g, g.title.get)), Some(selected), applyVisualization _, ("class" -> "form-control")) &
      "#script" #> Script(JsRaw(selected.script.get))
    }
  }

  
}

