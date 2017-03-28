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

class BillSection extends LongKeyedMapper[BillSection] with IdPK {
  def getSingleton = BillSection

  object dateCreated extends MappedDateTime(this)
  object hash extends MappedString(this, 64)
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object parent extends MappedLongForeignKey(this, BillSection) {
    override def dbIndexed_? = true
  }
  object bill extends MappedLongForeignKey(this, Bill) {
    override def dbIndexed_? = true
  }

  var children: List[BillSection] = Nil

}

object BillSection extends BillSection with LongKeyedMetaMapper[BillSection] {
  override def dbTableName = "billsection"

  override def unapply(a: Any): Option[BillSection] = BillSection.find(By(BillSection.hash, a.toString))

  def add(bill_id: String, parent: Option[BillSection]): Box[BillSection] = {
    for {
      user <- User.currentUser
      bill <- Bill.find(By(Bill.bill_id, bill_id))
    } yield {
      val newSection = BillSection.create.dateCreated(new java.util.Date())
                                    .creator(user).bill(bill)
      newSection.save

      val hash = (newSection.id.get + newSection.dateCreated.get.toString).crc32

      newSection.hash(hash)
      parent match {
        case Some(p) => newSection.parent(p)
        case _ => newSection.parent(newSection)
      }
      newSection.save
      Full(newSection)
    }

    Empty
  }
}