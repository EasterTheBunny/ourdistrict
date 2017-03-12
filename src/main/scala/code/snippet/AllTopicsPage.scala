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

import model.Topic

import net.liftweb._
import common._
import http._
import sitemap._
import util._
import Helpers._
import Loc._
import mapper._

import scala.xml.NodeSeq

object AllTopicsPage {
  // define the menu item for the page that
  // will display all items
  lazy val menu = Menu.i("Documents") / "documents" >> Loc.Snippet("Topics", render) >> LocGroup("navbar")
    
  // display the items
  def render =
    "#topics *" #> {
      List(S.param("p"), S.param("q"), S.param("s"), S.param("ord")) match {
        case List(Full(page), Full(a), Full(sortBy), Full(order)) => renderTopics(Topic.findAll(Like(Topic.details, "%" + a + "%"), OrderBy(Topic.nodeVoteTotal, Descending), StartAt(page.toLong * 50), MaxRows(50)))
        case List(Full(page), Full(a), Full(sortBy), Empty) => renderTopics(Topic.findAll(Like(Topic.details, "%" + a + "%"), OrderBy(Topic.nodeVoteTotal, Descending), StartAt(page.toLong * 50), MaxRows(50)))
        case List(Full(page), Full(a), Empty, Empty) => renderTopics(Topic.findAll(Like(Topic.details, "%" + a + "%"), OrderBy(Topic.nodeVoteTotal, Descending), StartAt(page.toLong * 50), MaxRows(50)))
        case List(Empty, Full(a), Empty, Empty) => renderTopics(Topic.findAll(Like(Topic.details, "%" + a + "%"), OrderBy(Topic.nodeVoteTotal, Descending), StartAt(0), MaxRows(50)))
        case List(Full(page), Empty, Empty, Empty) => renderTopics(Topic.findAll(OrderBy(Topic.nodeVoteTotal, Descending), StartAt(page.toLong * 50), MaxRows(50)))
        case _ => renderTopics(Topic.findAll(OrderBy(Topic.nodeVoteTotal, Descending), StartAt(0), MaxRows(50)))
      }
    }
    
  // for a list of items, display those items
  def renderTopics(in: Seq[Topic]) =
    "#topic" #> in.map(topic => {
      "class=panel-title *" #> <a href={TopicPage.menu.calcHref(topic)}>{ topic.title.get }</a> &
      "class=panel-body *" #> topic.details.get
    })
}