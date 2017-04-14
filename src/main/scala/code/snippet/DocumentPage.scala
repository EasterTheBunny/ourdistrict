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
package snippet

import java.io.File
import java.text.SimpleDateFormat

import net.liftweb._
import common._
import util._
import Helpers._
import http._
import sitemap._

import scala.xml.{Text,Node}
import code.model._
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.read
import net.liftweb.mapper.By

import scalaj.http.{Http,HttpResponse}

case class BillMeta(bill_version_id: String,
                    issued_on: String,
                    urls: BillURLs,
                    version_code: String)

case class BillURLs(html: String,
                    pdf: String,
                    unknown: String,
                    xml: String)

case class LastMod(xml: java.util.Date,
                   pdf: java.util.Date,
                   mods: java.util.Date,
                   text: java.util.Date)

object DocumentPage extends Loggable {

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  }

  def searchByBillId(id: String): Box[Box[Bill]] = {
    try{
      Full(Bill.find(By(Bill.bill_id, id)))
    } catch {
      case e: Exception => Full(Empty)
    }
  }

  def mostRecentBillMetaData(bill: Bill): Option[BillMeta] = {
    Props.get("scraper.data") match {
      case Full(base) => {
        val congress = bill.congress.get
        val billtype = bill.bill_type.get
        val billnum = bill.bill_id.get replaceAll( "-"+congress, "" )

        val dir = base :: congress :: "bills" :: billtype :: billnum :: "text-versions" :: Nil

        val lastModDir = (new File(dir.mkString("/"))).listFiles.filter(_.isDirectory).toList
            .map(actionDir => {
              val listOfFiles = actionDir.listFiles

              listOfFiles.find(f => f.getName == "lastmod.json" ) match {
                case Some(modfile) => {
                  val source = scala.io.Source.fromFile(modfile)
                  val lines = source.getLines mkString "\n"
                  source.close

                  (actionDir, Some(read[LastMod](lines)))
                }
                case _ => (actionDir, None)
              }
            }).filter(_._2.isDefined)
            .sortWith((a, b) => {
              a._2.map(_.xml).getOrElse(new java.util.Date(42))
                .after(b._2.map(_.xml).getOrElse(new java.util.Date(42)))
            }).headOption

        for {
          dir <- lastModDir
          datafile <- dir._1.listFiles.find(f => f.getName == "data.json")
        } yield {
          val source = scala.io.Source.fromFile(datafile)
          val lines = source.getLines mkString "\n"
          source.close

          read[BillMeta](lines)
        }
      }
      case _ => {
        logger.info("scraper data directory missing from config")
        None
      }
    }
  }

  // data extraction direct to value
  val nodesToExtract = "text" :: "enum" :: "header" :: "continuation-text" :: Nil

  // data that is not structured yet
  val nodesToStore = "graphic" :: "formula" :: "toc" :: "list" :: "table" :: Nil

  // data that is traversable
  val nodesToTraverse = "account" :: "subaccount" :: "subsubaccount" :: "subsubsubaccount" :: "title" :: "subtitle" ::
                        "part" :: "subpart" :: "chapter" :: "subchapter" :: "division" :: "subdivision" :: "section" ::
                        "subsection" :: "paragraph" :: "subparagraph" :: "clause" :: "subclause" :: "item" ::
                        "subitem" :: "appropriations–para" :: "quoted-block" :: Nil

  /*
		* BILLS
		*
    * ((account | appropriations–para | chapter | subdivision | division | subsection | paragraph | subparagraph | clause | subclause |
    *     item | subitem | part | section | subaccount | subchapter | subpart | subsubaccount | subsubsubaccount | subtitle | title |
    *     quoted–block | graphic | formula | toc | table | list | header | constitution–article | text)+, after–quoted–block)
    *   quoted-block
    *
    * (header, subheader*, appropriations–para*, (subaccount > subsubaccount > subsubsubaccount | section)*)
    *   account
    *
    * ((enum, header?, toc?), section*, ((subtitle | chapter | part)* | (account | subaccount)*)?)
    *   title
    *
    * ((enum, header?, toc?), section*, (chapter | part)*)
    *   subtitle
    *
    * ((enum, header?, toc?), section*, (subpart | chapter)*)
    *   part
    *
    * ((enum, header?, toc?), section*, (chapter)*)
    *   suppart
    *
    * ((enum, header?, toc?), section*, (subchapter | part)*)
    *   chapter
    *
    * ((enum, header?, toc?), section*, (part)*)
    *   subchapter
    *
    * ((enum, header?, toc?), section*, (subdivision | title)*)
    *   division
    *
    * ((enum, header?, toc?), section*, title*)
    *   subdivision
    *
    * (enum?, header?, (text | appropriations–para*)?, (quoted–block | graphic | formula | toc | table | list)*, (subsection* | paragraph* | committee–appointment–paragraph*), continuation–text?)
    *   section
    *
    * (enum, header?, (text? | appropriations–para*)?, (quoted–block | graphic | formula | toc | table | list)*, (paragraph | continuation–text)*)
    *   subsection
    *
    * (enum, header?, (text? | appropriations–para*)?, (quoted–block | graphic | formula | toc | table | list)*, (subparagraph | continuation–text)*)
    *   paragraph
    *
    * (enum, header?, (text? | appropriations–para*)?, (quoted–block | graphic | formula | toc | table | list)*, (clause | continuation–text)*)
    *   subparagraph
    *
    * (enum, header?, (text? | appropriations–para*)?, (quoted–block | graphic | formula | toc | table | list)*, (subclause | continuation–text)*)
    *   clause
    *
    * (enum, header?, (text? | appropriations–para*)?, (quoted–block | graphic | formula | toc | table | list)*, (item | continuation–text)*)
    *   subclause
    *
    * (enum, header?, (text? | appropriations–para*)?, (quoted–block | graphic | formula | toc | table | list)*, (subitem | continuation–text)*)
    *   item
    *
    * (enum, header?, (text? | appropriations–para*)?, (quoted–block | graphic | formula | toc | table | list)*, (subitem | continuation–text)*)
    *   subitem
    *
    * image-data
    *   graphic
    *
    * graphic
    *   formula
    *
    * ((header?, instructive–para?), ((toc–entry | toc–quoted–entry)+ | (multi–column–toc–entry | multi–column–toc–quoted–entry)+))+
    *   toc
    *
    * (ttitle*, tdesc*, tgroup+)
    *   table
    *
    * (list–item+)
    *   list
    *
    * (enum, section+)
    *   constitution-article
    *
    * appropriations–para:
    *   (text, proviso*)
    *
    */

	/*
	RESOLUTION
	(form, preamble?, resolution–body, official–title–amendment?, impeachment-resolution-signature?, attestation?, endorsement?)




	 */

  def menu = Menu.param[Box[Bill]]("Document",
                        Loc.LinkText(bill => Text(bill.map(_.bill_id.get).openOr("Init Document"))),
                        searchByBillId _,
                        _.map(_.bill_id.get).openOr("init_document")) / "document" / *
}

class DocumentPage(bill_maybe: Box[Bill]) extends Loggable {
  private def processBillLayer(bill: Bill, layer: Node, parent: Box[BillLayer] = Empty, quoted: Boolean = false): Unit = {
    val label = layer.label
    val to_quote = layer.label == "quoted-block" || quoted
    val need_to_store = DocumentPage.nodesToStore.contains(label)

    val db_layer = BillLayer.create.hash(layer \ "@id" openOr md5(nextFuncName))

    db_layer.creator(User.currentUser)
    db_layer.parent(parent)
    db_layer.layer_type(label)
    db_layer.dateCreated(new java.util.Date())
    db_layer.quoted(quoted)
    db_layer.bill(bill)

    layer \ "enum" map( e => db_layer.enum(e.text) )
    layer \ "header" map( h => db_layer.header(h.text) )
    layer \ "header" map( h => db_layer.header_raw(h.toString) )
    db_layer.text(layer \ "text" map( _.text ) mkString """\\n""")
    db_layer.text_raw(layer \ "text" map( _.toString ) mkString """\\n""")
    db_layer.proviso(layer \ "proviso" map( _.text ) mkString """\\n""")
    db_layer.proviso_raw(layer \ "proviso" map( _.toString ) mkString """\\n""")

    if(need_to_store) db_layer.layer_raw(layer.toString)

    db_layer.save

    if(!need_to_store) {
      layer.child.filter(n => {
        DocumentPage.nodesToStore.contains(n.label) ||
          DocumentPage.nodesToTraverse.contains(n.label)
      }).foreach(n => processBillLayer(bill = bill, layer = n, parent = Full(db_layer), quoted = to_quote))
    }
  }

  def render = {
    for {
      init <- S.param("initialize")
      bill <- bill_maybe
      meta <- DocumentPage.mostRecentBillMetaData(bill)
    } yield {
      if (!bill.initialized.get) {
        /**
          * read the source location, download the file, parse the data
          * and proceed with rendering
          */
        val response: HttpResponse[String] = Http(meta.urls.xml).asString
        response.code match {
          case 200 => {
            val billXML = xml.XML.loadString(response.body)

            // for bills
            (billXML \\ "legis-body").foreach(bdy =>
              bdy.child.foreach( n => processBillLayer(bill = bill, layer = n) ))

            // for resolutions
						(billXML \ "preamble").map(p => bill.preamble(p.toString))
						(billXML \\ "resolution-body").foreach(bdy =>
							bdy.child.foreach( n => processBillLayer(bill = bill, layer = n) ))


            bill.initialized(true).initFrom(meta.urls.xml).pdfLink(meta.urls.pdf).save
            S.redirectTo(DocumentPage.menu.calcHref(Full(bill)))
          }
          case _ => logger.warn(meta.urls.xml + " failed with response code: " + response.code)
        }

        println("done processing")
      }
    }

    bill_maybe match {
      case Full(bill) if bill.initialized.get =>
        "#bill_section" #> {
          for {
            template <- S.runTemplate("templates-hidden" :: "_documentCarousel" :: Nil)
          } yield {
            template
          }
        }
      case Full(bill) =>
        "@init_bill [href]" #> (DocumentPage.menu.calcHref(Full(bill)) + "?initialize")
      case _ =>
        "@headline *" #> "We didn't find anything." &
        "@headline_text *" #> "The bill you entered may not exist. What matters is that we don't have it." &
        "@init_bill *" #> "Search Again" &
        "@init_bill [href]" #> ("/" + SearchDocuments.menu.path.map(_.pathItem).mkString("/"))
    }
  }
}


