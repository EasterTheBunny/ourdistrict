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

class Committee extends LongKeyedMapper[Committee] with IdPK {
  def getSingleton = Committee
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object dateCreated extends MappedDateTime(this)
  object hash extends MappedString(this, 64) {
    override def defaultValue = randomString(maxLen).crc32
  }
  object name extends MappedString(this, 100)
  object committee_id extends MappedString(this, 25)
  object subcommittee extends MappedString(this, 200)
  object subcommittee_id extends MappedString(this, 25)
  
  def members: List[User] = CommitteeUser.findAll(By(CommitteeUser.committee, this.id.get)).map(cu => cu.user.obj.openOrThrowException("no user found for committee members"))
  def topics: List[Topic] = CommitteeTopic.findAll(By(CommitteeTopic.committee, this.id.get)).map(ct => ct.topic.obj.openOrThrowException("topic not found"))
}

object Committee extends Committee with LongKeyedMetaMapper[Committee] {
  override def dbTableName = "committee"

}