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
import net.liftweb.json._
import Serialization._
import JsonParser._

class Bill extends LongKeyedMapper[Bill] with IdPK {
  def getSingleton = Bill
  object introduced_at extends MappedDate(this)
  object bill_id extends MappedString(this, 25)
  object bill_type extends MappedString(this, 10)
  object by_request extends MappedBoolean(this)
  object congress extends MappedInt(this)
  object number extends MappedInt(this)
  object official_title extends MappedString(this, 5000)
  object popular_title extends MappedString(this, 5000)
  object short_title extends MappedString(this, 5000)
  object status extends MappedString(this, 50)
  object status_at extends MappedDate(this)
  object subjects_top_term extends MappedString(this, 100)
  object updated_at extends MappedDateTime(this)
  /*object actions extends MappedString(this, 20000) {
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class Reference(reference: Option[String], reference_type: Option[String])
    case class Action(acted_at: Option[String], references: List[Reference], status: Option[String], text: Option[String], action_type: Option[String])

    def apply(v: List[Action]) = super.apply(write(v))
  
    def asList: List[Action] = read[List[Action]](get)
  }*/
  object history extends MappedString(this, 5000){
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class History(active: Boolean, awaiting_signature: Boolean, enacted: Boolean, vetoed: Boolean)

    def apply(v: History) = super.apply(write(v))
  
    def asList: History = read[History](get)
  }
  object related_bills extends MappedString(this, 5000){
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class Related(bill_id: Option[String], reason: Option[String], related_type: Option[String])

    def apply(v: List[Related]) = super.apply(write(v))
  
    def asList: List[Related] = read[List[Related]](get)
  }
  object subjects extends MappedString(this, 20000){
    implicit val formats = DefaultFormats // Brings in default date formats etc.

    def apply(v: List[String]) = super.apply(write(v))
  
    def asList: List[String] = read[List[String]](get)
  }
  object summary extends MappedString(this, 500000) {
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class Summary(as: Option[String], date: Option[String], text: Option[String])

    def apply(v: Summary) = super.apply(write(v))
  
    def asList: Summary = read[Summary](get)
  }
  object titles extends MappedString(this, 100000) {
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class Title(as: Option[String], is_for_portion: Boolean, title: Option[String], title_type: Option[String])

    def apply(v: List[Title]) = super.apply(write(v))
  
    def asList: List[Title] = read[List[Title]](get)
  }
  object amendments extends MappedString(this, 100000) {
    
  }
  
  def sponsors = BillSponsor.findAll(By(BillSponsor.bill, this.id.get), By(BillSponsor.sponsorship, "sponsor")).map(_.sponsor.obj.openOrThrowException("something must have been deleted"))
  def cosponsors = BillSponsor.findAll(By(BillSponsor.bill, this.id.get), By(BillSponsor.sponsorship, "cosponsor")).map(_.sponsor.obj.openOrThrowException("something must have been deleted"))
  def committees = CommitteeBill.findAll(By(CommitteeBill.bill, this.id.get)).map(_.committee.obj.openOrThrowException("something must have been deleted"))
}

object Bill extends Bill with LongKeyedMetaMapper[Bill] {
  override def dbTableName = "bill"

}