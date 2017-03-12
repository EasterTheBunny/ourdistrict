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

import net.liftweb.mapper._
import net.liftweb._
import common._
import util._
import Helpers._
import http._
import sitemap._

import scala.xml.Text

import code.model.Soapbox

class SoapboxPage() {
    
  def render = 
  "article" #> Soapbox.findAll(BySql("datecreated > (CURRENT_TIMESTAMP - interval '7 days')", IHaveValidatedThisSQL("athughlett","2015-02-19")), OrderBy(Soapbox.dateCreated, Descending)).map(box => {
    "@title *" #> box.title &
    "@body *" #> box.body &
    "@name" #> <i>-&nbsp;{ box.creator.obj.map(_.username.get).openOr("anonymous") }</i>
  })

  
}

