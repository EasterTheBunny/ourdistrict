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
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonDSL._

class BillLayer extends LongKeyedMapper[BillLayer] with IdPK {
  def getSingleton = BillLayer

  object dateCreated extends MappedDateTime(this)
  object hash extends MappedString(this, 64)
  object layer_type extends MappedString(this, 100)
  object enum extends MappedString(this, 100)
  object header extends MappedText(this)
  object header_raw extends MappedText(this)
  object text extends MappedText(this)
  object text_raw extends MappedText(this)
  object proviso extends MappedText(this)
  object proviso_raw extends MappedText(this)
  object layer_raw extends MappedText(this)
  object quoted extends MappedBoolean(this)
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object parent extends MappedLongForeignKey(this, BillLayer) {
    override def dbIndexed_? = true
  }
  object bill extends MappedLongForeignKey(this, Bill) {
    override def dbIndexed_? = true
  }

  var children: List[BillLayer] = Nil

}

object BillLayer extends BillLayer with LongKeyedMetaMapper[BillLayer] {
  override def dbTableName = "billlayer"

  override def unapply(a: Any): Option[BillLayer] = BillLayer.find(By(BillLayer.hash, a.toString))

  def unapply(key: String, bill: Bill): Option[BillLayer] =
    BillLayer.find(By(BillLayer.hash, key), By(BillLayer.bill, bill))

  def toJSON (l: BillLayer): JValue = {
    ("type" -> "parts") ~
    ("id" -> l.hash.get) ~
    ("children" -> l.children.map(toJSON(_))) ~
    ("attributes" ->
      ("part_type" -> l.layer_type.get) ~
      ("enum" -> l.enum.get) ~
      ("text" -> l.text.get) ~
      ("header" -> l.header.get) ~
      ("proviso" -> l.proviso.get) ~
      ("quoted" -> l.quoted.get))
  }

  def toJSON (l: List[BillLayer]): JValue = {
    l.map(toJSON(_))
  }

  def layersForBill (bill: Bill): List[BillLayer] = {
    /**
      * we need to structure this as a nested tree for easy consumption
      * the client side
      */
    val layers = BillLayer.findAll(By(BillLayer.bill, bill))

    // set the hash lookup
    val lookup = layers.foldLeft(Map[String, BillLayer]())((mp, la) => {
      mp + (la.id.get.toString -> la)
    })

    // collapse the layers
    layers.reverse.foldLeft(List[BillLayer]())((rootList, la) => {
      if (la.parent.isDefined) {
        if (lookup.contains(la.parent.get.toString)) {
          lookup(la.parent.get.toString) children = lookup(la.parent.get.toString).children :+ la
        }

        rootList
      } else {
        rootList :+ la
      }
    })
  }
}
