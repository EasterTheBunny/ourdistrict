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

import scala.xml.{NodeSeq, Text}
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.mapper._
import code.model.{SortedTopic,SortedTopicCookie}
import Helpers._

class SortedTopicSnippet {
  
  private def getList = {
    
        S.findCookie("sortref") match {
          case Full(cookie) => {
            SortedTopicCookie.find(By(SortedTopicCookie.hash, cookie.value.getOrElse(""))) match {
              case Full(dbcookie) => {
                var sorted: List[SortedTopic] = List()
                val unsorted = SortedTopic.findForList(dbcookie.topics.asList)
                dbcookie.topics.asList.map(topic => {
                  sorted = sorted :+ unsorted.find(item => {
                    item.hash == topic
                  }).get
                })
                sorted
              }
              case _ => SortedTopic.findDefault
            }
          }
          case _ => {
            SortedTopic.findDefault
          }
        }
        
  }

  def writeList = "#item *" #> getList.map(d => {
    "@title" #> <b>{ d.title.get }</b> &
    "@list-hash [id]" #> d.hash.get &
    "@search [href]" #> ("/topic?q=" + d.title.get)
  })
  
}
