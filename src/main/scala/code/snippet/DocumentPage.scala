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

import net.liftweb._
import common._
import util._
import Helpers._
import http._
import sitemap._

import scala.xml.Text
import code.model._
import net.liftweb.mapper.By

object DocumentPage {
  def searchByBillId(id: String): Box[Box[Bill]] = {
    try{
      Full(Bill.find(By(Bill.bill_id, id)))
    } catch {
      case e: Exception => Full(Empty)
    }
  }

  def menu = Menu.param[Box[Bill]]("Document",
                        Loc.LinkText(bill => Text(bill.map(_.bill_id.get).openOr("Init Document"))),
                        searchByBillId _,
                        _.map(_.bill_id.get).openOr("init_document")) / "document" / *
}

class DocumentPage(bill_maybe: Box[Bill]) {

  def render = {
    for {
      init <- S.param("initialize")
      bill <- bill_maybe
    } yield {
      if (!bill.initialized.get) {
        /**
          * read the source location, download the file, parse the data
          * and proceed with rendering
          */



        //bill.initialized(true).save
      }
    }

    bill_maybe match {
      case Full(bill) if bill.initialized.get => {
        "#bill_section" #> bill.bill_id.get &
          "@bill-details" #> bill.official_title.get
      }
      case Full(bill) => {
        "@init_bill [href]" #> (DocumentPage.menu.calcHref(Full(bill)) + "?initialize")
      }
      case _ => {
        "@headline *" #> "We didn't find anything." &
        "@headline_text *" #> "The bill you entered may not exist. What matters is that we don't have it." &
        "@init_bill *" #> "Search Again" &
        "@init_bill [href]" #> ("/" + AllTopicsPage.menu.path.map(_.pathItem).mkString("/"))
      }
    }
  }
}


