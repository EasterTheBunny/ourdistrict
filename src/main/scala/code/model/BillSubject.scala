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

class BillSubject extends LongKeyedMapper[BillSubject] with IdPK {
  def getSingleton = BillSubject
  object bill extends MappedLongForeignKey(this, Bill) {
    override def dbIndexed_? = true
  }
  object subject extends MappedLongForeignKey(this, Subject) {
    override def dbIndexed_? = true
  }
}

object BillSubject extends BillSubject with LongKeyedMetaMapper[BillSubject] {
  override def dbTableName = "billsubject"

  def join (bill: Bill, subject: Subject) = {
    this.create.bill(bill).subject(subject).save
  }
}