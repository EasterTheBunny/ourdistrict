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

import code.mapper.MappedList

import com.roundeights.hasher.Implicits._

class SortedTopicCookie extends LongKeyedMapper[SortedTopicCookie] with IdPK {
  def getSingleton = SortedTopicCookie
  object hash extends MappedString(this, 64) {
    override val defaultValue = ""
  }
  object ipaccess extends MappedString(this, 25)
  object useragent extends MappedString(this, 2000)
  object topics extends MappedList(this, 2000) {
    override def defaultValue = "[]"
  }
  
}

object SortedTopicCookie extends SortedTopicCookie with LongKeyedMetaMapper[SortedTopicCookie] {
  override def dbTableName = "sortedtopiccookie"

  def findOrCreateCookie(i_hash: String) = SortedTopicCookie.find(By(SortedTopicCookie.hash, i_hash)) match {
    case Full(cookie) => cookie
    case _ => SortedTopicCookie.create.hash(randomString(SortedTopicCookie.hash.maxLen).crc32)
  }
}