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
//import JsonParser._
import JsonDSL._

import code.model.{Sponsor}

import java.text.SimpleDateFormat
import java.io.File
import java.util.Date

import org.joda.time.DateTime
import org.yaml.snakeyaml._
import scala.collection.JavaConversions._

object ParseLegislatorsToDB extends LiftActor with Loggable {

  case class ParseLegislators()
  case class Stop()
  
  private var stopped = false
  private var skipFirst = Props.get("scraper.skipInitOnBoot").getOrElse("false").toBoolean

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-SSS'Z'")
  }
  
  def messageHandler = {
    case ParseLegislators if !stopped => {
      Schedule.schedule(this, ParseLegislators, 30 days)

      if(!skipFirst) {
        Props.get("scraper.legislators") match {
          case Full(base) => {
            /**
              * this bit is messy, but it works
              * so... i'm leaving it for now
              */
            val source = scala.io.Source.fromFile(base)
            val lines = source.getLines.toList
            source.close

            val yaml = new Yaml()
            val load = yaml.load(lines.mkString("\n")).asInstanceOf[java.util.ArrayList[Map[Any, Any]]]
            List(load.toArray(): _*).map(c => {
              val sponsor = scala.collection.mutable.Map[String, Any]();

              val d = c.asInstanceOf[java.util.LinkedHashMap[String, java.util.LinkedHashMap[String, Object]]]
              var g: scala.collection.mutable.Map[String, java.util.LinkedHashMap[String, Object]] = d
              //println(g)

              g.map(obj => {
                if (obj._1 == "id") {
                  var a: scala.collection.mutable.Map[String, Object] = obj._2
                  sponsor += ("bioguide" -> a.get("bioguide").asInstanceOf[Option[String]].getOrElse(""))
                  sponsor += ("thomas" -> a.get("thomas").asInstanceOf[Option[String]].getOrElse(""))
                  sponsor += ("lis" -> a.get("lis").asInstanceOf[Option[String]].getOrElse(""))
                  sponsor += ("govtrack" -> a.get("govtrack").asInstanceOf[Option[Int]].getOrElse(-1))
                  sponsor += ("opensecrets" -> a.get("opensecrets").asInstanceOf[Option[String]].getOrElse(""))
                  sponsor += ("votesmart" -> a.get("votesmart").asInstanceOf[Option[Int]].getOrElse(-1))
                  sponsor += ("cspan" -> a.get("cspan").asInstanceOf[Option[Int]].getOrElse(-1))
                  sponsor += ("wikipedia" -> a.get("wikipedia").asInstanceOf[Option[String]].getOrElse(""))
                }
                else if (obj._1 == "name") {
                  var a: scala.collection.mutable.Map[String, Object] = obj._2
                  sponsor += ("first" -> a.get("first").asInstanceOf[Option[String]].getOrElse(""))
                  sponsor += ("last" -> a.get("last").asInstanceOf[Option[String]].getOrElse(""))
                  sponsor += ("full" -> a.get("full").asInstanceOf[Option[String]].getOrElse(""))
                }
                else if (obj._1 == "bio") {
                  var a: scala.collection.mutable.Map[String, Object] = obj._2
                  sponsor += ("birthday" -> a.get("birthday").asInstanceOf[Option[String]].getOrElse(""))
                  sponsor += ("gender" -> a.get("gender").asInstanceOf[Option[String]].getOrElse(""))
                  sponsor += ("religion" -> a.get("religion").asInstanceOf[Option[String]].getOrElse(""))
                }
                else if (obj._1 == "terms") {
                  val a = List(obj._2.asInstanceOf[java.util.ArrayList[Map[Any, Any]]].toArray(): _*)

                  a.map(term => {
                    var term_d: scala.collection.mutable.Map[String, Object] = term.asInstanceOf[java.util.LinkedHashMap[String, Object]]
                    val end = DateTime parse term_d("end").asInstanceOf[String]
                    if (end isAfterNow()) {
                      sponsor += ("type" -> term_d.get("type").asInstanceOf[Option[String]].getOrElse(""))
                      sponsor += ("start" -> DateTime.parse(term_d("start").asInstanceOf[String]))
                      sponsor += ("end" -> end)
                      sponsor += ("state" -> term_d("state").asInstanceOf[String])
                      sponsor += ("district" -> term_d.get("district").asInstanceOf[Option[Int]].getOrElse(-1))
                      sponsor += ("party" -> term_d("party").asInstanceOf[String])
                    }
                  })
                }
              })

              val sp = Sponsor.find(By(Sponsor.bioguide_id, sponsor("bioguide").asInstanceOf[String])) match {
                case Full(s) => s
                case _ => Sponsor.create.thomas_id(sponsor("thomas").asInstanceOf[String])
                  .district(sponsor("district").asInstanceOf[Int])
                  .state(sponsor("state").asInstanceOf[String])
              }

              sp.last_name(sponsor("last").asInstanceOf[String])
                .first_name(sponsor("first").asInstanceOf[String])
                .birthday(DateTime.parse(sponsor("birthday").asInstanceOf[String]).toDate())
                .gender(sponsor("gender").asInstanceOf[String] match {
                  case "M" => Genders.Male
                  case "F" => Genders.Female
                })
                .`type`(sponsor("type").asInstanceOf[String])
                .state(sponsor("state").asInstanceOf[String])
                .party(sponsor("party").asInstanceOf[String])
                .religion(sponsor("religion").asInstanceOf[String])
                .term_start(sponsor("start").asInstanceOf[DateTime].toDate)
                .term_end(sponsor("end").asInstanceOf[DateTime].toDate)

              sp.validate match {
                case Nil => sp.save
                case errors:List[FieldError] => println(errors)
              }
            })
          }
          case _ => logger.info("scraper legislator file missing from config")
        }
      } else skipFirst = false

      ParseBillsToDB ! ParseBillsToDB.ParseBills
  	}
    case Stop => stopped = true
  }
  
}