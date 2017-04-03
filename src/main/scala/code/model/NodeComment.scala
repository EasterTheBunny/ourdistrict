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
import Helpers._
import code.mapper.MappedList
import com.roundeights.hasher.Implicits._
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonDSL._

class NodeComment extends LongKeyedMapper[NodeComment] with IdPK {
  def getSingleton = NodeComment

  object text extends MappedText(this)
  object dateCreated extends MappedDateTime(this)
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object node extends MappedLongForeignKey(this, LayerNode)
  object hash extends MappedString(this, 64)
  object vote extends MappedInt(this)
  object upvotes extends MappedList(this) {
    override def defaultValue = "[]"
  }
  object downvotes extends MappedList(this) {
    override def defaultValue = "[]"
  }
  object parent extends MappedLongForeignKey(this, NodeComment) {
    override def dbIndexed_? = true
  }
  object parentHash extends MappedString(this, 64)

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

  var children: List[NodeComment] = Nil
}

object NodeComment extends NodeComment with LongKeyedMetaMapper[NodeComment] {
  override def dbTableName = "nodecomment"

  def add(text: String, node: LayerNode, parent: Box[NodeComment]): NodeComment = {
    val comment = NodeComment.create.text(text.trim()).dateCreated(new java.util.Date())
                              .node(node).vote(1).creator(User.currentUser)
                              .hash((nextFuncName + text).crc32)

    User.currentUser.map( u => comment.upvotes.add(u.id.get.toString) )

    parent match {
      case Full(p) => comment.parent(p).parentHash(p.hash.get)
      case _ => {
        comment.save
        comment.parent(comment).parentHash(comment.hash.get)
      }
    }

    comment.save
    comment
  }

  def toJSON (c: NodeComment): JValue = {
    ("id" -> c.hash.get) ~
    ("parent" -> c.parentHash.get) ~
    ("vote" -> c.vote.get) ~
    ("text" -> c.text.get) ~
    ("uservote" -> c.voteForCurrentUser_?) ~
    ("user" -> c.creator.obj.map(_.username.get).openOr("unknown")) ~
    ("children" -> toJSON(c))
  }

  def toJSON (c: List[NodeComment]): JValue = {
    c.map(toJSON(_))
  }

}