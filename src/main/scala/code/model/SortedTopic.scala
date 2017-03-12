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
import Helpers._

import com.roundeights.hasher.Implicits._

class SortedTopic extends LongKeyedMapper[SortedTopic] with IdPK {
  def getSingleton = SortedTopic
  object title extends MappedString(this, 200)
  object hash extends MappedString(this, 64) {
    override val defaultValue = ""
  }
  object rank extends MappedInt(this) {
    override def defaultValue = 0
  }
  object writeins extends MappedInt(this) {
    override def defaultValue = 0
  }
  
  def canlist = if(writeins.get > 10) true else false
  
}

object SortedTopic extends SortedTopic with LongKeyedMetaMapper[SortedTopic] {
  override def dbTableName = "sortedtopic"

  def findDefault = SortedTopic.findAll(By_>(SortedTopic.writeins, 10), OrderBy(SortedTopic.rank, Ascending), MaxRows(10))
  def findSuggestForString(k: String) = SortedTopic.findAll(Like(SortedTopic.title, k), OrderBy(SortedTopic.writeins, Descending), MaxRows(10))
  def findForList(l: List[String]) = SortedTopic.findAll(ByList(SortedTopic.hash, l), OrderBy(SortedTopic.rank, Ascending))
  
  def findOrCreateTopic(title: String) = SortedTopic.find(By(SortedTopic.title, title)) match {
    case Full(topic) => topic
    case _ => SortedTopic.create.title(title).hash(randomString(SortedTopic.hash.maxLen).crc32)
  }
  
}