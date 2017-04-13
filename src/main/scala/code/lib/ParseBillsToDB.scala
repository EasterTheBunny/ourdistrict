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

import scala.collection.mutable
import net.liftweb.json._
import Serialization._
import JsonDSL._

import code.model._
import code.mapper.Sponsors

import java.text.SimpleDateFormat
import java.io.File

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

case class JSONCommittee(activity: List[String],
                         committee: String,
                         committee_id: String,
                         subcommittee: Option[String],
                         subcommittee_id: Option[String])

case class JSONSponsor(bioguide_id: Option[String],
                       district: Option[String],
                       name: String,
                       state: String,
                       original_sponsor: Option[Boolean],
                       sponsored_at: Option[String],
                       title: String,
                       `type`: Option[String],
                       withdrawn_at: Option[String])

case class JSONAction(acted_at: String,
                      text: String,
                      `type`: String)

case class JSONBill(actions: List[JSONAction],
                    amendments: JArray,
                    bill_id: String,
                    bill_type: String,
                    by_request: Boolean,
                    committees: List[JSONCommittee],
                    congress: String,
                    cosponsors: List[JSONSponsor],
                    enacted_as: Option[JObject],
                    history: JObject,
                    introduced_at: String,
                    number: String,
                    official_title: String,
                    popular_title: Option[String],
                    related_bills: JArray,
                    short_title: Option[String],
                    sponsor: JSONSponsor,
                    status: String,
                    status_at: String,
                    subjects: List[String],
                    subjects_top_term: String,
                    summary: JObject,
                    titles: JArray,
                    updated_at: String)

trait JSONBillExtraction {
  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-SSS'Z'")
  }

  protected var allBillInfo: List[Bill] = Nil
  protected var allActionInfo: List[Action] = Nil
  protected var allCommitteeBillInfo: List[CommitteeBill] = Nil
  protected var allSponsorBillInfo: List[BillSponsor] = Nil
  protected var allCosponsorBillInfo: List[BillSponsor] = Nil
  protected val timestampLookup: mutable.HashMap[String, mutable.HashMap[String, Bill]] = mutable.HashMap()
  private var allCommitteeInfo: List[Committee] = Nil
  private var allSponsorInfo: List[Sponsor] = Nil
  private var allSubjectInfo: List[Subject] = Nil

  def parseJSONFileToBill(f: File) = {
    val source = scala.io.Source.fromFile(f)
    val lines = source.getLines mkString "\n"
    source.close

    read[JSONBill](lines)
  }

  protected def dateFromModFile(f: File) = {
    val data = scala.io.Source.fromFile(f).getLines mkString ""

    val dtFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dtFormatter.parseDateTime( data )
  }

  protected def initializeLookups = {
    /**
      * bill lookup is structured so that as we move through the directory structure
      * we can test if a particular bill has been added to the database and when
      *
      * bill_type ->
      * bill_id -> last update
      */
    allBillInfo.foreach(b => {
      if(!timestampLookup.contains(b.bill_type.get))
        timestampLookup.update(b.bill_type.get, mutable.HashMap())

      if(!timestampLookup.apply(b.bill_type.get).contains(b.bill_id.get))
        timestampLookup.apply(b.bill_type.get).update(b.bill_id.get, b)
    })

    allCommitteeInfo = Committee.findAllFields(Seq[SelectableField](Committee.committee_id))
    allSponsorInfo = Sponsor.findAllFields(Seq[SelectableField](Sponsor.bioguide_id,
                                                                Sponsor.name,
                                                                Sponsor.state,
                                                                Sponsor.district,
                                                                Sponsor.title))
    allSubjectInfo = Subject.findAll

    timestampLookup
  }

  protected def mergeBillCaseAndDB(data: (JSONBill, Option[Bill]), skipCheck: Boolean = false) = {
    val billCase = data._1
    val billDB = data._2

    /**
      * create or update bill
      */
    val theBill = billDB.getOrElse(Bill.create).introduced_at(DateTime.parse(billCase.introduced_at).toDate)
      .amendments(prettyRender(billCase.amendments))
      .bill_id(billCase.bill_id)
      .bill_type(billCase.bill_type)
      .by_request(billCase.by_request)
      .congress(billCase.congress.toInt)
      .history(prettyRender(billCase.history))
      .number(billCase.number.toInt)
      .official_title(billCase.official_title)
      .popular_title(billCase.popular_title.getOrElse(""))
      .related_bills(prettyRender(billCase.related_bills))
      .short_title(billCase.short_title.getOrElse(""))
      .status(billCase.status)
      .status_at(DateTime.parse(billCase.status_at).toDate)
      .subjects_top_term(billCase.subjects_top_term)
      .summary(prettyRender(billCase.summary))
      .titles(prettyRender(billCase.titles))
      .updated_at(DateTime.parse(billCase.updated_at).toDate)
      .last_scrape(new java.util.Date())

    theBill.save

    if(billCase.subjects.nonEmpty) {
      billCase.subjects.foreach(s => {
        allSubjectInfo.find(_.text.get.toLowerCase == s.trim.toLowerCase) match {
          case Some(found) => BillSubject.join(theBill, found)
          case _ =>
            val sub = Subject.create.text(s)
            sub.save
            allSubjectInfo = allSubjectInfo :+ sub
            BillSubject.join(theBill, sub)
        }
      })
    }

    mergeActionsAndDB(data = (billCase, theBill), skipCheck = skipCheck)
    mergeCommitteesAndDB(data = (billCase, theBill), skipCheck = skipCheck)
    mergeSponsorsAndDB(data = (billCase, theBill))
    mergeCosponsorsAndDB(data = (billCase, theBill))
  }

  private def mergeActionsAndDB(data: (JSONBill, Bill), skipCheck: Boolean = false) = {
    val billCase = data._1
    val billDB = data._2

    /**
      * actions could already be saved:
      *
      * if a bill was in existence we need to compare with
      * what we have
      *
      */
    val toInsert = skipCheck match {
      case false =>
        val checkAgainst = allActionInfo.filter(_.bill.get == billDB.id.get)

        billCase.actions.filter( b => {
          !checkAgainst.exists(_.acted_at.get == b.acted_at)
        })
      case true =>
        billCase.actions
    }

    toInsert.map(action => {
      Action.create.acted_at(DateTime.parse(action.acted_at).toDate)
        .bill(billDB)
        .text(action.text)
        .`type`(action.`type`)
        .save
    })
  }

  private def mergeCommitteesAndDB(data: (JSONBill, Bill), skipCheck: Boolean = false) = {
    val billCase = data._1
    val billDB = data._2

    val checkAgainst = allCommitteeBillInfo.filter(_.bill.get == billDB.id.get)

    def addCommitteeLink(committee: Option[Committee], bill: Bill, caseCommittee: JSONCommittee) = {
      val c_exists = committee.isDefined

      val theCom = committee.getOrElse(Committee.create)
        theCom.name(caseCommittee.committee)
              .committee_id(caseCommittee.committee_id)
              .subcommittee(caseCommittee.subcommittee.getOrElse(""))
              .subcommittee_id(caseCommittee.subcommittee_id.getOrElse(""))
              .save

      if(!c_exists)
        allCommitteeInfo = theCom +: allCommitteeInfo

      CommitteeBill.join(bill, theCom, prettyRender(caseCommittee.activity))
    }

    /**
      * committees could already be saved:
      *
      * if a bill was in existence we need to compare with
      * what we have
      *
      */
    skipCheck match {
      case false =>
        billCase.committees.foreach( b => {
          val committeeFound = allCommitteeInfo.find(_.committee_id.get == b.committee_id)

          committeeFound match {
            case Some(c) =>
              /**
                * the committee exists; check if the committee is
                * linked to this bill
                */
              if(!checkAgainst.exists(a => { a.committee.get == c.id.get && a.bill.get == billDB.id.get }))
                addCommitteeLink(Some(c), billDB, b)
            case None =>
              /**
                * here, the connection of a bill to a committee
                * is not made because the committee doesn't exist
                */
              addCommitteeLink(None, billDB, b)
          }
        })
      case true =>
        billCase.committees.map(cm => addCommitteeLink(None, billDB, cm))
    }
  }

  private def mergeSponsorsAndDB(data: (JSONBill, Bill)) = {
    val billCase = data._1
    val billDB = data._2

    val sponsor = allSponsorInfo.find(s => {
      /**
        * need a finer search and match here. names cannot be
        * a reliable match alone
        */

      // a bioguide id may not exist, but it should be unique if it does
      val b_id = billCase.sponsor.bioguide_id
      b_id match {
        case Some(bid) if s.bioguide_id.get != "" =>
          s.bioguide_id.get == bid
        case _ =>
          // our fallback will be to match state, name, and type
          // (though it may still not be enough)
          s.name.get == billCase.sponsor.name.trim &&
            s.state.get.toUpperCase == billCase.sponsor.state.toUpperCase &&
            s.title.get.toUpperCase == billCase.sponsor.title.toUpperCase
      }
    })

    val s_exists = sponsor.isDefined

    val theSpons = sponsor.getOrElse(Sponsor.create)
      theSpons.name(billCase.sponsor.name.trim)
              .state(billCase.sponsor.state.toUpperCase)
              .title(billCase.sponsor.title.toUpperCase)

    billCase.sponsor.district.map(d => theSpons.district(d.toInt))
    billCase.sponsor.bioguide_id.map(b => theSpons.bioguide_id(b))
    billCase.sponsor.`type`.map(t => theSpons.sponsor_type(t))

      theSpons.save

    if(!s_exists)
      allSponsorInfo = theSpons +: allSponsorInfo

    allSponsorBillInfo.find(s => s.bill.get == billDB.id.get && s.sponsor.get == theSpons.id.get) match {
      case Some(s) =>
        /**
          * the sponsor exists and is connected; do nothing
          */
      case None =>
        /**
          * this sponsor bill connection doesn't exist; create it
          */
        BillSponsor.join(billDB, theSpons, Sponsors.Sponsor, billDB.introduced_at.get, null)
    }
  }

  private def mergeCosponsorsAndDB(data: (JSONBill, Bill)) = {
    val billCase = data._1
    val billDB = data._2

    val checkAgainst = allCosponsorBillInfo.filter(_.bill.get == billDB.id.get)

    def addCosponsorLink(cosponsor: Option[Sponsor], bill: Bill, caseCosponsor: JSONSponsor) = {
      val withdrawn = caseCosponsor.withdrawn_at match {
        case Some(w) => DateTime.parse(w).toDate()
        case None => null
      }

      val s_exists = cosponsor.isDefined

      val theSpons = cosponsor.getOrElse(Sponsor.create)
        theSpons.name(caseCosponsor.name.trim)
                  .state(caseCosponsor.state.toUpperCase)
                  .title(caseCosponsor.title.toUpperCase)

      caseCosponsor.district.map(d => theSpons.district(d.toInt))
      caseCosponsor.bioguide_id.map(b => theSpons.bioguide_id(b))

      theSpons.save

      if(!s_exists)
        allSponsorInfo = theSpons +: allSponsorInfo

      BillSponsor.join(bill, theSpons, Sponsors.Cosponsor, caseCosponsor.sponsored_at.map(d => DateTime.parse(d).toDate).orNull, withdrawn)
    }

    billCase.cosponsors.foreach(cs => {
      val cosponsorFound = allSponsorInfo.find(s => {
        /**
          * need a finer search and match here. names cannot be
          * a reliable match alone
          */
        // a bioguide id may not exist, but it should be unique if it does
        val b_id = cs.bioguide_id
        b_id match {
          case Some(bid) if s.bioguide_id.get != "" =>
            s.bioguide_id.get == bid
          case _ =>
            // our fallback will be to match state, name, and type
            // (though it may still not be enough)
            s.name.get == cs.name.trim &&
              s.state.get.toUpperCase == cs.state.toUpperCase &&
              s.title.get.toUpperCase == cs.title.toUpperCase
        }
      })

      cosponsorFound match {
        case Some(c) =>

          /**
            * the cosponsor exists; check if the cosponsor is
            * linked to this bill
            */
          if (!checkAgainst.exists(a => {
            a.sponsor.get == c.id.get && a.bill.get == billDB.id.get
          }))
            addCosponsorLink(Some(c), billDB, cs)
        case None =>

          /**
            * here, the connection of a bill to a cosponsor
            * is not made because the cosponsor doesn't exist
            */
          addCosponsorLink(None, billDB, cs)
      }
    })
  }
}

object ParseBillsToDB extends LiftActor with Loggable with JSONBillExtraction {
  
  case class ParseBills()
  case class Stop()
  
  private var stopped = false
  private var skipFirst = Props.get("scraper.skipInitOnBoot").getOrElse("false").toBoolean
  private val congress = Props.get("settings.congress").getOrElse("115")
  
  def messageHandler = {
    case ParseBills if !stopped =>
      Schedule.schedule(this, ParseBills, 7 days)

      if(!skipFirst) {
        /**
          * this query covers the entire scope of inserts
          * that MIGHT happen here.
          */
        allBillInfo = Bill.findAllFields(Seq[SelectableField](Bill.id, Bill.bill_type, Bill.bill_id, Bill.last_scrape),
                                By(Bill.congress, congress.toInt))

        /**
          * if there are no bills found for this congress
          * we can safely assume that all bills we scrape
          * need to be inserted
          */
        val firstPass = allBillInfo.length == 0

        /**
          * if bills are found, a lookup structure must be
          * created to aid in merging the database with
          * scraped bills
          */
        if(!firstPass) {
          allActionInfo = Action.findAllFields(Seq[SelectableField](Action.acted_at, Action.`type`, Action.bill),
            In(Action.bill,
              Bill.id,
              By(Bill.congress, congress.toInt)))

          allCommitteeBillInfo = CommitteeBill.findAllFields(Seq[SelectableField](CommitteeBill.bill, CommitteeBill.committee),
            In(CommitteeBill.bill,
              Bill.id,
              By(Bill.congress, congress.toInt)))

          allSponsorBillInfo = BillSponsor.findAllFields(Seq[SelectableField](BillSponsor.bill, BillSponsor.sponsor),
            In(BillSponsor.bill,
              Bill.id,
              By(Bill.congress, congress.toInt)), By(BillSponsor.sponsorship, Sponsors.Sponsor))

          allCosponsorBillInfo = BillSponsor.findAllFields(Seq[SelectableField](BillSponsor.bill, BillSponsor.sponsor),
            In(BillSponsor.bill,
              Bill.id,
              By(Bill.congress, congress.toInt)), By(BillSponsor.sponsorship, Sponsors.Cosponsor))

          /**
            * setup the lookup structure
            */
          initializeLookups
        }

        Props.get("scraper.data") match {
          case Full(base) => {
            val loc = base + "/" + congress + "/bills"
            (new File(loc)).listFiles.filter(_.isDirectory).toList
              .foreach( typeDir => {
                val billType = typeDir.getName

                typeDir.listFiles.filter(_.isDirectory)
                  .foreach( billDir => {
                    val listOfFiles = billDir.listFiles
                    val dataFile = listOfFiles.find(f => f.getName == "data.json" )

                    /**
                      * we now have a bill type and can build a bill_id
                      *
                      * test if we already have this bill and whether
                      * or not it is up to date
                      */
                    val billID = billDir.getName + "-" + congress
                    val lastModFile = listOfFiles.find(f => f.getName == "data-fromfdsys-lastmod.txt" )

                    timestampLookup.get(billType).map(_.get(billID)) match {
                      case Some(Some(bill)) =>
                        /**
                          * we have a bill by this type and id
                          * test if it needs to be updated
                          */
                        lastModFile match {
                          case Some(f) if dateFromModFile(f).isAfter(bill.last_scrape.get.getTime) => {
                            logger.debug(bill.bill_id.get)
                            mergeBillCaseAndDB((parseJSONFileToBill(f), Some(bill)), skipCheck = false)
                          }
                          case Some(f) => logger.info("bill not modified")
                          case _ => logger.warn("fdsys last mod file not found: " + billID)
                        }
                      case _ =>
                        /**
                          * bill not available in database, create it
                          */
                        dataFile match {
                          case Some(f) => {
                            logger.debug(billDir.getName)
                            mergeBillCaseAndDB((parseJSONFileToBill(f), None), skipCheck = true)
                          }
                          case _ => logger.warn("data.json file missing: " + billDir.getName)
                        }
                    }

                  })
              })
          }
          case _ => logger.info("scraper data directory missing from config")
        }
      } else skipFirst = false
      
    case Stop => stopped = true
  }
}