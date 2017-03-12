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
import http._
import common._
import util.Helpers._
import scala.xml.NodeSeq

class OnSubmit extends StatefulSnippet {
  
  private var name = ""
  private var age = "0"
    
  private val whence = S.referer openOr "/"
  
  def dispatch = {case "render" => render}
  
  def render =
    "name=name" #> SHtml.text(name, name = _, "id" -> "the_name") &
    "name=age" #> SHtml.text(age, age = _) &
    "type=submit" #> SHtml.onSubmitUnit(process)
  
  private def process() =
    
    asInt(age) match {
      case Full(a) if a < 13 => S.error("Too young!")
      case Full(a) => {
        S.notice("Name: "+name)
        S.notice("Age: "+age)
        S.redirectTo(whence)
      }
      case _ => S.error("Age doesn't parse as a number")
    }
}