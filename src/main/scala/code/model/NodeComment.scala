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
import Helpers._
import code.mapper.MappedList
import com.roundeights.hasher.Implicits._
import net.liftweb.json.DefaultFormats
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

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-SSS'Z'")
  }

  /**
    * Convert a JValue to a NodeComment if possible
    */
  def apply(in: JValue): Box[NodeComment] = tryo{in.extract[NodeComment]}

  override def unapply(a: Any): Option[NodeComment] = NodeComment.find(By(NodeComment.hash, a.toString))

  /**
    * Extract a JValue to a NodeComment
    */
  def unapply(in: JValue): Option[NodeComment] = apply(in)

  def add(text: String, node: LayerNode, parent: Box[NodeComment]): Box[NodeComment] = {
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
    Full(comment)
  }

  def toJSON (c: NodeComment): JValue = {
    ("type" -> "comments") ~
    ("id" -> c.hash.get) ~
    ("children" -> toJSON(c)) ~
    ("attributes" ->
      ("parent" -> c.parentHash.get) ~
      ("vote" -> c.vote.get) ~
      ("text" -> c.text.get) ~
      ("uservote" -> c.voteForCurrentUser_?) ~
      ("user" -> c.creator.obj.map(_.username.get).openOr("unknown")))
  }

  def toJSON (c: List[NodeComment]): JValue = {
    c.map(toJSON(_))
  }

  def commentsForNode(node: LayerNode): List[NodeComment] = {
    val comments = NodeComment.findAll(By(NodeComment.node, node))

    /**
      * we need to structure this as a nested tree for easy consumption
      * the client side
      */
    // set the hash lookup
    val lookup = comments.foldLeft(Map[String, NodeComment]())((mp, nc) => {
      mp + (nc.hash.get -> nc)
    })

    // collapse the nodes
    comments.foldLeft(List[NodeComment]())((obj, nc) => {
      if(lookup.contains(nc.parentHash.get) && nc.parent.get != nc.id.get) {
        lookup(nc.parentHash.get) children = lookup(nc.parentHash.get).children :+ nc
      }

      if(nc.parent.get == nc.id.get) obj :+ nc
      else obj
    })
  }

}