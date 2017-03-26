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

class Sponsor extends LongKeyedMapper[Sponsor] with IdPK {
  def getSingleton = Sponsor
  object district extends MappedInt(this)
  object name extends MappedString(this, 255)
  object state extends MappedString(this, 4)
  object thomas_id extends MappedString(this, 15) {
    override def dbIndexed_? = true
  }
  object title extends MappedString(this, 30)
  object sponsor_type extends MappedString(this, 25)
  
  object last_name extends MappedString(this, 255)
  object first_name extends MappedString(this, 255)
  object birthday extends MappedDate(this)
  object gender extends MappedGender(this)
  object `type` extends MappedString(this, 30)
  object party extends MappedString(this, 100)
  object url extends MappedString(this, 255)
  object address extends MappedString(this, 255)
  object phone extends MappedString(this, 25)
  object contact_form extends MappedString(this, 255)
  object rss_url extends MappedString(this, 255)
  object twitter extends MappedString(this, 100)
  object facebook extends MappedString(this, 255)
  object facebook_id extends MappedInt(this)
  object youtube extends MappedString(this, 100)
  object youtube_id extends MappedString(this, 255)
  object bioguide_id extends MappedString(this, 100)
  object opensecrets_id extends MappedString(this, 100)
  object lis_id extends MappedString(this, 100)
  object cspan_id extends MappedInt(this)
  object govtrack_id extends MappedInt(this)
  object votesmart_id extends MappedInt(this)
  object ballotpedia_id extends MappedString(this, 100)
  object washington_post_id extends MappedString(this, 100)
  object icpsr_id extends MappedInt(this)
  object wikipedia_id extends MappedString(this, 100)
  object term_start extends MappedDate(this)
  object term_end extends MappedDate(this)
  object religion extends MappedString(this, 100)
  
  def countProposedBills = BillSponsor.count(By(BillSponsor.sponsor, this.id.get),
		  									By(BillSponsor.congress, Props.get("settings.congress").getOrElse("115").toInt),
		  									By(BillSponsor.sponsorship, "sponsor"))
		  									/*
		  									 * 
		  									NotBy(BillSponsor.bill_type, "hconres"),
		  									NotBy(BillSponsor.bill_type, "hjres"),
		  									NotBy(BillSponsor.bill_type, "hres"),
		  									NotBy(BillSponsor.bill_type, "sconres"),
		  									NotBy(BillSponsor.bill_type, "sres"),
		  									NotBy(BillSponsor.bill_type, "sjres"),
		  									 */
}

object Sponsor extends Sponsor with LongKeyedMetaMapper[Sponsor] {
  override def dbTableName = "sponsor"

}