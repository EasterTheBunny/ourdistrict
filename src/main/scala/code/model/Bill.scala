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

import code.mapper.Sponsors

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.json._
import Serialization._
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonDSL._

class Bill extends LongKeyedMapper[Bill] with IdPK {
  def getSingleton = Bill
  object introduced_at extends MappedDate(this)
  object bill_id extends MappedString(this, 25) {
    override def dbIndexed_? = true
  }
  object bill_type extends MappedString(this, 10)
  object by_request extends MappedBoolean(this)
  object congress extends MappedInt(this) {
    override def dbIndexed_? = true
  }
  object number extends MappedInt(this)
	object preamble extends MappedText(this)
  object official_title extends MappedText(this)
  object popular_title extends MappedText(this)
  object short_title extends MappedText(this)
  object status extends MappedString(this, 50)
  object status_at extends MappedDate(this)
  object subjects_top_term extends MappedText(this)
  object updated_at extends MappedDateTime(this)
  object last_scrape extends MappedDateTime(this)
  /*object actions extends MappedString(this, 20000) {
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class Reference(reference: Option[String], reference_type: Option[String])
    case class Action(acted_at: Option[String], references: List[Reference], status: Option[String], text: Option[String], action_type: Option[String])

    def apply(v: List[Action]) = super.apply(write(v))

    def asList: List[Action] = read[List[Action]](get)
  }*/
  object history extends MappedText(this){
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class History(active: Boolean, awaiting_signature: Boolean, enacted: Boolean, vetoed: Boolean)

    def apply(v: History) = super.apply(write(v))

    def asList: History = read[History](get)
  }
  object related_bills extends MappedText(this){
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class Related(bill_id: Option[String], reason: Option[String], related_type: Option[String])

    def apply(v: List[Related]) = super.apply(write(v))

    def asList: List[Related] = read[List[Related]](get)
  }
  object summary extends MappedText(this) {
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class Summary(as: Option[String], date: Option[String], text: Option[String])

    def apply(v: Summary) = super.apply(write(v))

    def asList: Summary = read[Summary](get)
  }
  object titles extends MappedText(this) {
    implicit val formats = DefaultFormats // Brings in default date formats etc.
    case class Title(as: Option[String], is_for_portion: Boolean, title: Option[String], title_type: Option[String])

    def apply(v: List[Title]) = super.apply(write(v))

    def asList: List[Title] = read[List[Title]](get)
  }
  object amendments extends MappedText(this)
  object initialized extends MappedBoolean(this) {
    override def defaultValue: Boolean = false
  }
  object initFrom extends MappedString(this, 255)
  object pdfLink extends MappedString(this, 255)


  def sponsors = BillSponsor.findAll(By(BillSponsor.bill, this), By(BillSponsor.sponsorship, Sponsors.Sponsor)).map(_.sponsor.obj.openOrThrowException("something must have been deleted"))
  def cosponsors = BillSponsor.findAll(By(BillSponsor.bill, this), By(BillSponsor.sponsorship, Sponsors.Cosponsor)).map(_.sponsor.obj.openOrThrowException("something must have been deleted"))
  def committees = CommitteeBill.findAll(By(CommitteeBill.bill, this)).map(_.committee.obj.openOrThrowException("something must have been deleted"))
  def subjects = BillSubject.findAll(By(BillSubject.bill, this)).map(_.subject.obj.openOrThrowException("something must have been deleted"))

  def layerForBill(key: String) = BillLayer.find(By(BillLayer.hash, key), By(BillLayer.bill, this))
}

object Bill extends Bill with LongKeyedMetaMapper[Bill] {
  override def dbTableName = "bill"

  def unapply(key: String): Option[Bill] = Bill.find(By(Bill.bill_id, key))

  def billTimestampInfoForCongress(congress: Integer): List[Bill] =
    Bill.findAllFields(Seq[SelectableField](Bill.id, Bill.bill_type, Bill.bill_id, Bill.last_scrape),
                        By(Bill.congress, congress))

  def toJSON (b: Bill): JValue = {
    ("type" -> "bills") ~
    ("id" -> b.bill_id.get) ~
    ("attributes" ->
      ("bill_id" -> b.bill_id.get) ~
      ("congress" -> b.congress.get) ~
      ("official_title" -> b.official_title.get) ~
      ("popular_title" -> b.popular_title.get) ~
      ("short_title" -> b.short_title.get) ~
      ("summary" -> b.summary.get) ~
      ("pdf" -> b.pdfLink.get) ~
			("preamble" -> b.preamble.get))
  }

  def toJSON (n: List[Bill]): JValue = {
    n.map(toJSON(_))
  }
}
