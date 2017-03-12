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

import scala.xml.{NodeSeq, Text}
import net.liftweb._
import http._
import common._
import util.Helpers._

import code.model._

class AddEssay extends StatefulSnippet {
  def dispatch = {
    case "addEssay" => add _
  }
  
  def add(in: NodeSeq) : NodeSeq = User.currentUser match {
    case Full(user) => Text("")
    case _ => Text("")
  }
}