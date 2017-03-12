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
package lib

import net.liftweb.util.Schedule
import net.liftweb.actor.LiftActor
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util._
import Helpers._
//import net.liftweb.json.JsonAST._
import net.liftweb.json._
import Serialization._
import JsonParser._
import JsonDSL._

import code.model.{Bill,Committee,Sponsor,CommitteeBill,BillSponsor,Action}

import java.text.SimpleDateFormat
import java.io.File
import java.util.Date

import org.joda.time.DateTime

object ParseBillsToDB extends LiftActor {
  
  case class ParseBills()
  case class Stop()
  
  private var stopped = false
  private var skipFirst = true
  
  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-SSS'Z'")
  }
  
  def messageHandler = {
    case ParseBills if !stopped =>
      Schedule.schedule(this, ParseBills, 7 days)
      if(!skipFirst) {
        val loc = Props.mode match {
          case Props.RunModes.Production => "/usr/local/govtrack/congress/114/bills"
          case _ => "/Users/athughlett/Documents/GovTrack/congress/114/bills"
        }
        
    	val filesArray = recursiveListFiles(new File(loc)).map(f => parseJsonAndInsert(f))
    	ParseLegislatorsToDB ! ParseLegislatorsToDB.ParseLegislators
      } else skipFirst = false
      
    case Stop => stopped = true
  }
  
  def recursiveListFiles(f: File): Array[File] = {
    val files = f.listFiles()
    val good = files.filter(f => """data\.json$""".r.findFirstIn(f.getName).isDefined)
    good ++ files.filter(_.isDirectory()).flatMap(recursiveListFiles)
  }
  
  def parseJsonAndInsert(f: File) = {
    val source = scala.io.Source.fromFile(f)
    val lines = source.getLines mkString "\n"
    source.close
    
    case class JSONBillOne(actions: JArray, amendments: JArray, bill_id: String, bill_type: String, by_request: Boolean, committees: JArray, congress: String, cosponsors: JArray,
    						enacted_as: Option[String], history: JObject, introduced_at: String, number: String, official_title: String, popular_title: Option[String], related_bills: JArray,
    						short_title: Option[String], sponsor: JObject, status: String, status_at: String, subjects: JArray, subjects_top_term: String)
    						
    case class JSONBillTwo(summary: JObject, titles: JArray, updated_at: String)
    case class JSONCommittee(activity: List[String], committee: String, committee_id: String, subcommittee: Option[String], subcommittee_id: Option[String])
    case class JSONCosponsor(district: Option[String], name: String, sponsored_at: String, state: String, thomas_id: String, title: String, withdrawn_at: Option[String])
    case class JSONSponsor(district: Option[String], name: String, state: String, thomas_id: String, title: String, `type`: String)
    case class JSONAction(acted_at: String, in_committee: Option[String], how: Option[String], bill_ids: Option[JArray], committees: Option[JArray], calendar: Option[String], number: Option[String], references: Option[JArray],
							result: Option[String], roll: Option[String], status: Option[String], suspension: Option[String],
							text: String, `type`: String, vote_type: Option[String], where: Option[String], under: Option[String])
    val billPartOne = read[JSONBillOne](lines)
    val billPartTwo = read[JSONBillTwo](lines)
    
    val actions = billPartOne.actions.extract[List[JSONAction]]
    val committees = billPartOne.committees.extract[List[JSONCommittee]]
    val cosponsors = billPartOne.cosponsors.extract[List[JSONCosponsor]]
    val sponsor = billPartOne.sponsor.extract[JSONSponsor]
    
    //println(billPartOne.bill_id + " length: " + pretty(render(billPartTwo.summary)).length)
    
    val bill = Bill.create.introduced_at(DateTime.parse(billPartOne.introduced_at).toDate())
    			//.actions(pretty(render(billPartOne.actions)))
    			.amendments(pretty(render(billPartOne.amendments)))
    			.bill_id(billPartOne.bill_id)
    			.bill_type(billPartOne.bill_type)
    			.by_request(billPartOne.by_request)
    			.congress(billPartOne.congress.toInt)
    			.history(pretty(render(billPartOne.history)))
    			.number(billPartOne.number.toInt)
    			.official_title(billPartOne.official_title)
    			.popular_title(billPartOne.popular_title.getOrElse(""))
    			.related_bills(pretty(render(billPartOne.related_bills)))
    			.short_title(billPartOne.short_title.getOrElse(""))
    			.status(billPartOne.status)
    			.status_at(DateTime.parse(billPartOne.status_at).toDate())
    			.subjects(pretty(render(billPartOne.subjects)))
    			.subjects_top_term(billPartOne.subjects_top_term)
    			.summary(pretty(render(billPartTwo.summary)))
    			.titles(pretty(render(billPartTwo.titles)))
    			.updated_at(DateTime.parse(billPartTwo.updated_at).toDate())
    
    bill.save
    
    actions.map(action => {
      Action.create.acted_at(DateTime.parse(action.acted_at).toDate())
      				.in_committee(action.in_committee.getOrElse(""))
      				.bill(bill)
      				.bill_ids(action.bill_ids match{
      				  case Some(e) => pretty(render(e))
      				  case _ => ""
      				})
      				.calendar(action.calendar.getOrElse(""))
      				.committees(action.committees match {
      				  case Some(c) => pretty(render(c))
      				  case _ => ""
      				})
      				.how(action.how.getOrElse(""))
      				.number(action.number.getOrElse(""))
      				.references(action.references match {
      				  case Some(r) => pretty(render(r))
      				  case _ => ""
      				})
      				.result(action.result.getOrElse(""))
      				.roll(action.roll.getOrElse(""))
      				.status(action.status.getOrElse(""))
      				.suspension(action.suspension.getOrElse(""))
      				.text(action.text)
      				.`type`(action.`type`)
      				.vote_type(action.vote_type.getOrElse(""))
      				.where(action.where.getOrElse(""))
      				.under(action.under.getOrElse(""))
      				.save
    })
    
    committees.map(c => {
      Committee.find(By(Committee.committee_id, c.committee_id)) match {
        case Full(committee) =>
          committee.name(c.committee)
          			.subcommittee(c.subcommittee.getOrElse(""))
          			.subcommittee_id(c.subcommittee_id.getOrElse(""))
          			.save
          			
          CommitteeBill.join(bill, committee, pretty(render(c.activity)))
        case _ => 
          val committee = Committee.create.name(c.committee)
          									.committee_id(c.committee_id)
          									.subcommittee(c.subcommittee.getOrElse(""))
          									.subcommittee_id(c.subcommittee_id.getOrElse(""))
          committee.save
          CommitteeBill.join(bill, committee, pretty(render(c.activity)))
      }
    })	
    
    cosponsors.map(cs => {
      val withdrawn = cs.withdrawn_at match {
        case Some(w) => DateTime.parse(w).toDate()
        case None => null
      }
      
      Sponsor.find(By(Sponsor.thomas_id, cs.thomas_id)) match {
        case Full(cosponsor) =>
          BillSponsor.join(bill, cosponsor, "cosponsor", DateTime.parse(cs.sponsored_at).toDate(), withdrawn)
        case _ => 
          val sponsor = Sponsor.create.district(cs.district.getOrElse("-1").toInt)
        						.name(cs.name)
        						.state(cs.state)
        						.thomas_id(cs.thomas_id)
        						.title(cs.title)
          sponsor.save
          
          BillSponsor.join(bill, sponsor, "cosponsor", DateTime.parse(cs.sponsored_at).toDate(), withdrawn)
      }
    })
    
    Sponsor.find(By(Sponsor.thomas_id, sponsor.thomas_id)) match {
      case Full(sp) =>
        sp.sponsor_type(sponsor.`type`).save
        BillSponsor.join(bill, sp, "sponsor", bill.introduced_at.get, null)
      case _ => 
        val spon = Sponsor.create.district(sponsor.district.getOrElse("-1").toInt)
        							.name(sponsor.name)
        							.state(sponsor.state)
        							.thomas_id(sponsor.thomas_id)
        							.title(sponsor.title)
        							.sponsor_type(sponsor.`type`)
        spon.save
        BillSponsor.join(bill, spon, "sponsor", bill.introduced_at.get, null)
    }
  }
}