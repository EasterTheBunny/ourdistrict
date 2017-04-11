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
import common._
import http._
import rest._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._


object AutocompleteHandler extends RestHelper with Loggable {

  def toJSON (n: List[Subject]): JValue = {
    n.map(s => {
      ("id" -> s.id.get) ~
      ("value" -> s.text.get)
    })
  }

  serve( "ajax" / "autocomplete" prefix {
    case "subjects" :: Nil JsonGet _ => {
      for {
        term <- S.param("term") ?~ "query param required" ~> 400
      } yield {
        toJSON(Subject.findAll(Like(Subject.text, "%"+term+"%"),
          OrderBy(Subject.text, Ascending), MaxRows(100)))
      }
    }
  })
}