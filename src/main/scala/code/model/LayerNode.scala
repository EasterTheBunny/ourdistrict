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

import java.text.SimpleDateFormat

import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util._
import net.liftweb.http._
import Helpers._
import code.mapper.MappedList
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, ISODateTimeFormat}
import com.roundeights.hasher.Implicits._
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonDSL._

class LayerNode extends LongKeyedMapper[LayerNode] with IdPK {
  def getSingleton = LayerNode

  object statement extends MappedString(this, 500)
  object details extends MappedString(this, 2000)
  object dateCreated extends MappedDateTime(this)
  object vote extends MappedInt(this)
  object nodeID extends MappedInt(this)
  object hash extends MappedString(this, 64)
  object upvotes extends MappedList(this) {
    override def defaultValue = "[]"
  }
  object downvotes extends MappedList(this) {
    override def defaultValue = "[]"
  }
  object commentCount extends MappedInt(this)
  object commentVoteTotal extends MappedInt(this)
  object layer extends MappedLongForeignKey(this, BillLayer) {
    override def dbIndexed_? = true
  }
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object parent extends MappedLongForeignKey(this, LayerNode) {
    override def dbIndexed_? = true
  }
  object parentHash extends MappedString(this, 64)

  def comments: List[Comment] = {
    Comment.findAll(By(Comment.node, this.id.get))
  }

  def voteForCurrentUser_?(): Int = {
    User.currentUser match {
      case Full(user) =>
        if(upvotes.asList.contains(user.id.get.toString)) 1
        else if(downvotes.asList.contains(user.id.get.toString)) -1
        else 0

      case _ => 0
    }
  }

  def voteUpForUser(user: User) = {
    downvotes.remove(user.id.get.toString).save
    upvotes.add(user.id.get.toString).save
  }

  def voteDownForUser(user: User) = {
    upvotes.remove(user.id.get.toString).save
    downvotes.add(user.id.get.toString).save
  }

  var children: List[LayerNode] = Nil

}

object LayerNode extends LayerNode with LongKeyedMetaMapper[LayerNode] {
  override def dbTableName = "layernode"

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-SSS'Z'")
  }

  /**
    * Convert a JValue to a LayerNode if possible
    */
  def apply(in: JValue): Box[LayerNode] = tryo{in.extract[LayerNode]}

  override def unapply(a: Any): Option[LayerNode] = LayerNode.find(By(LayerNode.hash, a.toString))

  /**
    * Extract a JValue to a LayerNode
    */
  def unapply(in: JValue): Option[LayerNode] = apply(in)

  def findById(id: Int): Box[LayerNode] = {
    for {
      node <- LayerNode.find(By(LayerNode.nodeID, id)) ?~ "Node not found"
    } yield node
  }

  def add(statement: String, details: String, layer: BillLayer, parent: Box[LayerNode]): Box[LayerNode] = {
    val newnode = LayerNode.create.statement(statement).layer(layer)
                            .dateCreated(new java.util.Date()).creator(User.currentUser)
                            .details(details)

    User.currentUser match {
      case Full(u) => newnode.vote(1).upvotes.add(u.id.get.toString)
      case _ => newnode.vote(0)
    }

    val hash = (nextFuncName + newnode.statement.get).crc32
    newnode.hash(hash)

    newnode.validate match {
      case Nil =>
        parent match {
          case Full(p) => newnode.parent(p).parentHash(p.hash.get)
          case _ => {
            newnode.save
            newnode.parent(newnode).parentHash(newnode.hash.get)
          }
        }

        newnode.save
        Full(newnode)
      case errors: List[FieldError] =>
        S.error(errors)
        Empty ?~ "validation errors"
    }
  }

  def toJSON (n: LayerNode): JValue = {
    ("type" -> "nodes") ~
    ("id" -> n.hash.get) ~
    ("attributes" ->
      ("statement" -> n.statement.get) ~
      ("details" -> n.details.get) ~
      ("vote" -> n.vote.get) ~
      ("uservote" -> n.voteForCurrentUser_?) ~
      ("date" -> DateTimeFormat.forPattern("MMMM e, yyyy HH:mm:ss").print(new DateTime(n.dateCreated.get))) ~
      ("parent" -> n.parentHash.get) ~
      ("user" -> n.creator.obj.map(_.username.get).openOr("phmfic")) ~
      ("children" -> n.children.map(toJSON(_))))
  }

  def toJSON (n: List[LayerNode]): JValue = {
    n.map(toJSON(_))
  }

  def nodesForLayer(layer: BillLayer): List[LayerNode] = {
    val nodes = LayerNode.findAll(By(LayerNode.layer, layer))

    /**
      * we need to structure this as a nested tree for easy consumption
      * the client side
      */
    // set the hash lookup
    val lookup = nodes.foldLeft(Map[String, LayerNode]())((mp, li) => {
      mp + (li.hash.get -> li)
    })

    // collapse the nodes
    nodes.foldLeft(List[LayerNode]())((rootNodes, li) => {
      if (lookup.contains(li.parentHash.get) && li.parent.get != li.id.get) {
        lookup(li.parentHash.get) children = lookup(li.parentHash.get).children :+ li
      }

      if (li.parent.get == li.id.get) rootNodes :+ li
      else rootNodes
    })
  }
}