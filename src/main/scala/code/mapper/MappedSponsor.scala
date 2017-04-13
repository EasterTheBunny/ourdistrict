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

import net.liftweb.mapper._
import net.liftweb.http._

abstract class MappedSponsor[T <: Mapper[T]](owner: T) extends MappedEnum(owner, Sponsors) {
  override def defaultValue = Sponsors.Sponsor
}

object Sponsors extends Enumeration {
  val Sponsor = new I18NSponsor(1, "Sponsor")
  val Cosponsor = new I18NSponsor(2, "Cosponsor")

  class I18NSponsor(id: Int, name: String) extends Val(id, name) {
    override def toString = {
      S.?(name)
    }
  }
}