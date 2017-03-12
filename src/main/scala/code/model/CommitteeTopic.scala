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

class CommitteeTopic extends LongKeyedMapper[CommitteeTopic] with IdPK {
  def getSingleton = CommitteeTopic
  object committee extends MappedLongForeignKey(this, Committee)
  object topic extends MappedLongForeignKey(this, Topic)
  object dateIntroduced extends MappedDateTime(this)
  object vote extends MappedInt(this) {
    override def defaultValue = 0
  }
  
}

object CommitteeTopic extends CommitteeTopic with LongKeyedMetaMapper[CommitteeTopic] {
  override def dbTableName = "committeetopic"

  def join (committee: Committee, topic: Topic) = this.create.committee(committee).topic(topic).dateIntroduced(new java.util.Date()).save
}