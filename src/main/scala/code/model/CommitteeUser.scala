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

class CommitteeUser extends LongKeyedMapper[CommitteeUser] with IdPK {
  def getSingleton = CommitteeUser
  object committee extends MappedLongForeignKey(this, Committee)
  object user extends MappedLongForeignKey(this, User)
  object dateJoined extends MappedDateTime(this)
  
}

object CommitteeUser extends CommitteeUser with LongKeyedMetaMapper[CommitteeUser] {
  override def dbTableName = "committeeuser"

  def join (committee: Committee, user: User) = this.create.committee(committee).user(user).dateJoined(new java.util.Date()).save
}