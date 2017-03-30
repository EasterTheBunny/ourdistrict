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

import com.roundeights.hasher.Implicits._

class BillLayer extends LongKeyedMapper[BillLayer] with IdPK {
  def getSingleton = BillLayer

  object dateCreated extends MappedDateTime(this)
  object hash extends MappedString(this, 64)
  object layer_type extends MappedString(this, 100)
  object enum extends MappedString(this, 100)
  object header extends MappedText(this)
  object header_raw extends MappedText(this)
  object text extends MappedText(this)
  object text_raw extends MappedText(this)
  object proviso extends MappedText(this)
  object proviso_raw extends MappedText(this)
  object layer_raw extends MappedText(this)
  object quoted extends MappedBoolean(this)
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object parent extends MappedLongForeignKey(this, BillLayer) {
    override def dbIndexed_? = true
  }
  object bill extends MappedLongForeignKey(this, Bill) {
    override def dbIndexed_? = true
  }

  var children: List[BillLayer] = Nil

}

object BillLayer extends BillLayer with LongKeyedMetaMapper[BillLayer] {
  override def dbTableName = "billlayer"

  override def unapply(a: Any): Option[BillLayer] = BillLayer.find(By(BillLayer.hash, a.toString))
}