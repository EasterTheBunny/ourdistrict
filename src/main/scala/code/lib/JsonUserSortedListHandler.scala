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
package lib

import model._
import net.liftweb._
import mapper._
import util._
import common._
import TimeHelpers._
import http._
import rest._
import json._
import provider.HTTPCookie

import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._


object JsonUserSortedListHandler extends RestHelper {
  def toJSON (n: List[SortedTopic]): JValue = {
    n.map(t => {
      (("label" -> t.title.get))
    })
  }
  
  serve( "json" / "sortedlist" prefix {
    case "suggest" :: _ Get _ => 
      for{
        term <- S.param("term") ?~ "term parameter missing" ~> 400
      } yield {
        toJSON(SortedTopic.findSuggestForString("%" + term + "%"))
      }
      
    case "add" :: _ Post _ =>
      for{
        term <- S.param("term") ?~ "term parameter missing" ~> 400
      } yield {
        var out: JValue = ("status" -> "success")
        
        try{
          val newTopic = SortedTopic.findOrCreateTopic(term)
          newTopic.writeins(newTopic.writeins.get + 1).save
          out = ("status" -> "success") ~ ("value" -> newTopic.hash.get)
        } catch {
          case _: Throwable => out = ("status" -> "internal server error")
        }
        
        out
      }
      
    case "sort" :: _ Get _ =>
      for{
        sortorder <- S.param("o") ?~ "sort order parameter missing" ~> 400
        request <- S.containerRequest
      } yield {
        val cookie = S.findCookie("topicsortorder") match {
          case Full(cookie) => cookie
          case _ => new HTTPCookie("topicsortorder", Full("empty"), Empty, Full("/"), Full(31557600), Empty, Empty, Empty) //new HTTPCookie("topicsortorder", Full("empty"), Empty, Empty, Full(31557600), Full(1), Empty, Empty)
        }
        
        val sortList = sortorder.split(",").toList
        val dbcookie = SortedTopicCookie.findOrCreateCookie(cookie.value.getOrElse(""))
        dbcookie.ipaccess(request.remoteAddress).useragent(request.userAgent openOr null).topics(sortList).save
        
        S.addCookie(cookie.setValue(dbcookie.hash.get))
        S.addCookie(new HTTPCookie("sortref", Full(dbcookie.hash.get), Empty, Full("/"), Full(31557600), Empty, Empty, Empty))
        val ret: JValue = ("cookie" -> cookie.value.openOr(""))
        ret
      }
  })
}