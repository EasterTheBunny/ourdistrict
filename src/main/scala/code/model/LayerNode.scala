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
import net.liftweb.json._
import Helpers._
import code.mapper.MappedList
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, ISODateTimeFormat}
import com.roundeights.hasher.Implicits._
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

  override def unapply(a: Any): Option[LayerNode] = LayerNode.find(By(LayerNode.hash, a.toString))

  def findById(id: Int): Box[LayerNode] = {
    for {
      node <- LayerNode.find(By(LayerNode.nodeID, id)) ?~ "Node not found"
    } yield node
  }

  def add(statement: String, details: String, layer: BillLayer, parent: Box[LayerNode]): LayerNode = {
    val newnode = LayerNode.create.statement(statement).layer(layer)
                            .dateCreated(new java.util.Date()).creator(User.currentUser)
                            .details(details).vote(1)

    User.currentUser match {
      case Full(u) => newnode.upvotes.add(u.id.get.toString)
      case _ =>
    }

    val hash = (nextFuncName + newnode.statement.get).crc32
    newnode.hash(hash)

    parent match {
      case Full(p) => newnode.parent(p).parentHash(p.hash.get)
      case _ => {
        newnode.save
        newnode.parent(newnode).parentHash(newnode.hash.get)
      }
    }

    newnode.save
    newnode
  }

  def toJSON (n: LayerNode): JValue = {
    ("statement" -> n.statement.get) ~
    ("details" -> n.details.get) ~
    ("vote" -> n.vote.get) ~
    ("uservote" -> n.voteForCurrentUser_?) ~
    ("date" -> DateTimeFormat.forPattern("MMMM e, yyyy HH:mm:ss").print(new DateTime(n.dateCreated.get))) ~
    ("parent" -> n.parentHash.get) ~
    ("id" -> n.hash.get) ~
    ("user" -> n.creator.obj.map(_.username.get).openOr("unknown")) ~
    ("children" -> n.children.map(toJSON(_)))
  }

  def toJSON (n: List[LayerNode]): JValue = {
    n.map(toJSON(_))
  }
}