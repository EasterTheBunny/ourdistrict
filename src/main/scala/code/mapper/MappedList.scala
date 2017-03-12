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
package mapper

import scala.xml.{NodeSeq, Elem}
import net.liftweb.http.S
import net.liftweb.http.S._
import net.liftweb.util._
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.json._
import Serialization._
import JsonParser._

abstract class MappedList[T<:Mapper[T]](owner : T, maxLen: Int) extends MappedString[T](owner, maxLen) {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  def apply(v: List[String]) = super.apply(write(v))
  
  def asList: List[String] = read[List[String]](get)
  
  def remove(s: String) = {
    val lst = asList diff List(s)
    apply(lst)
  }
  
  def add(s: String) = {
    apply(asList :+ s)
  }
}