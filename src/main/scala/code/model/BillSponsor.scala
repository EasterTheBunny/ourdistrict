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

class BillSponsor extends LongKeyedMapper[BillSponsor] with IdPK {
  def getSingleton = BillSponsor
  object bill extends MappedLongForeignKey(this, Bill)
  object sponsor extends MappedLongForeignKey(this, Sponsor)
  object sponsor_at extends MappedDate(this)
  object withdrawn_at extends MappedDate(this)
  object sponsorship extends MappedString(this, 25)
  object bill_type extends MappedString(this, 10)
  object congress extends MappedInt(this)
}

object BillSponsor extends BillSponsor with LongKeyedMetaMapper[BillSponsor] {
  override def dbTableName = "billsponsor"

  def join (bill: Bill, sponsor: Sponsor, sponsorship: String, sponsor_at: java.util.Date, withdrawn_at: java.util.Date) = {
    this.create.bill(bill).sponsor(sponsor).sponsorship(sponsorship).sponsor_at(sponsor_at).withdrawn_at(withdrawn_at).bill_type(bill.bill_type.get).congress(bill.congress.get).save
  }
}