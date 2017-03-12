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

import model._
import net.liftweb._
import mapper._
import util._
import common._
import TimeHelpers._
import http._
import rest._
import json._

import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat


object JsonDataHandler extends RestHelper {
  
  def toJSON (n: List[Sponsor]): JValue = {
    n.map(s => {
    	  (("name" -> (s.first_name.get + " " + s.last_name.get)) ~
    	  ("state" -> s.state.get) ~
    	  ("sort" -> 1) ~
    	  ("party" -> s.party.get) ~
    	  ("bills_proposed" -> s.countProposedBills))
    	})
  }
  
  serve {
    case "json" :: "senate" :: "billpiedata" :: Nil JsonGet _ => toJSON(Sponsor.findAll(By(Sponsor.district, -1),
    																	BySql("term_end >= CURRENT_DATE", IHaveValidatedThisSQL("athughlett","2015-03-02")),
    																	OrderBy(Sponsor.state, Ascending)))
    case "json" :: "house" :: "billpiedata" :: Nil JsonGet _ => toJSON(Sponsor.findAll(By_>(Sponsor.district, -1),
    																	BySql("term_end >= CURRENT_DATE", IHaveValidatedThisSQL("athughlett","2015-03-02")),
    																	OrderBy(Sponsor.state, Ascending)))
  }
}