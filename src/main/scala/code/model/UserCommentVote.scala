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

import java.sql.Types
import net.liftweb._
import mapper._

class UserCommentVote extends LongKeyedMapper[UserCommentVote] with IdPK {
  def getSingleton = UserCommentVote

  object vote extends MappedInt(this) {
    override def targetSQLType: Int = Types.TINYINT
  }
  object dateRecorded extends MappedDateTime(this)
  object user extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object comment extends MappedLongForeignKey(this, NodeComment) {
    override def dbIndexed_? : Boolean = true
  }

  /* (vote == 1 or vote == -1) */
  object check extends MappedBoolean(this) {
    override def defaultValue: Boolean = false
  }

}

object UserCommentVote extends UserCommentVote with LongKeyedMetaMapper[UserCommentVote] {
  override def dbTableName = "usercommentvote"
}