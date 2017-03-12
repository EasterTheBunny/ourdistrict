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

class Action extends LongKeyedMapper[Action] with IdPK {
  //case class JSONAction(acted_at: String, how: Option[String], bill_ids: Option[List[String]], committees: Option[List[String]], calendar: Option[String], number: Option[String], references: JArray,
	//						result: Option[String], roll: Option[String], status: Option[String], suspension: Option[String],
	//						text: String, `type`: String, vote_type: Option[String], where: Option[String], under: Option[String])
    
  def getSingleton = Action
  object bill extends MappedLongForeignKey(this, Bill) {
    override def dbIndexed_? = true
  }
  object acted_at extends MappedDateTime(this)
  object in_committee extends MappedString(this, 255)
  object how extends MappedString(this, 100)
  object bill_ids extends MappedString(this, 1000)
  object committees extends MappedString(this, 1000)
  object calendar extends MappedString(this, 255)
  object number extends MappedString(this, 50)
  object references extends MappedString(this, 1000)
  object result extends MappedString(this, 50)
  object roll extends MappedString(this, 50)
  object status extends MappedString(this, 255)
  object suspension extends MappedString(this, 255)
  object text extends MappedString(this, 2000)
  object `type` extends MappedString(this, 100)
  object vote_type extends MappedString(this, 100)
  object where extends MappedString(this, 50)
  object under extends MappedString(this, 255)
}

object Action extends Action with LongKeyedMetaMapper[Action] {
  override def dbTableName = "action"

}