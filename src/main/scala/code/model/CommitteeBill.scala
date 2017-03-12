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
package model

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util._
import net.liftweb.http._
import net.liftweb.json._
import Serialization._
import JsonParser._

class CommitteeBill extends LongKeyedMapper[CommitteeBill] with IdPK {
  def getSingleton = CommitteeBill
  object bill extends MappedLongForeignKey(this, Bill)
  object committee extends MappedLongForeignKey(this, Committee)
  object activity extends MappedString(this, 2000){
    implicit val formats = DefaultFormats // Brings in default date formats etc.

    def apply(v: List[String]) = super.apply(write(v))
  
    def asList: List[String] = read[List[String]](get)
  }
  
}

object CommitteeBill extends CommitteeBill with LongKeyedMetaMapper[CommitteeBill] {
  override def dbTableName = "committeebill"

  def join (bill: Bill, committee: Committee, activity: String) = this.create.bill(bill).committee(committee).activity(activity).save
}