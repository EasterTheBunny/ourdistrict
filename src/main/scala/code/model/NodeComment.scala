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

import net.liftweb._
import common._
import mapper._
import util._
import Helpers._

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
  object upVotes extends MappedInt(this)
  object downVotes extends MappedInt(this)
  object parent extends MappedLongForeignKey(this, NodeComment) {
    override def dbIndexed_? = true
  }
  object parentHash extends MappedString(this, 64)

  def voteForCurrentUser_?(): Int = {
    User.currentUser match {
      case Full(user) =>  0
      case _ => 0
    }
  }

  def currentUserVote: Box[UserCommentVote] =
    User.currentUser.map(u =>
      UserCommentVote.find(By(UserCommentVote.user, u),
                           By(UserCommentVote.comment, this))) openOr Empty

  def doVote(vote: Int): Any = {
    /**
      * vote (absolute):
      *   1 = upvote
      *   0 = neutral vote
      *   -1 = downvote
      */
    val validVote = if(vote > 1) 1 else if(vote < -1) -1 else vote
    val theVote = currentUserVote match {
      case Full(v) =>
        val oldVote = v.vote.get
        v.vote(validVote).check(validVote != 0).dateRecorded(new java.util.Date()).save

        if(oldVote != 0) (oldVote - v.vote.get) * -1
        else oldVote - v.vote.get

      case _ =>
        UserCommentVote.create.vote(validVote).dateRecorded(new java.util.Date())
          .user(User.currentUser).comment(this).check(validVote != 0).save

        validVote
    }

    val up = if(theVote > 0) 1 else if(theVote < -1) -1 else 0
    val down = if(theVote > 1) -1 else if(theVote < 0) 1 else 0
    val stmtc = "update "+getSingleton.dbTableName+" set vote = vote + " +
                theVote+", upvotes = upvotes + "+up+", downvotes = downvotes + " +
                down+" where "+getSingleton.dbTableName+".id = "+this.id.get

    DB.runUpdate(stmtc, Nil, getSingleton.dbDefaultConnectionIdentifier)
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

    parent match {
      case Full(p) => comment.parent(p).parentHash(p.hash.get)
      case _ => {
        comment.save
        comment.parent(comment).parentHash(comment.hash.get)
      }
    }

    comment.save

    User.currentUser.map( u => {
      UserCommentVote.create.user(u).dateRecorded(new java.util.Date()).check(true).save
      comment.doVote(1)
    })

    Full(comment)
  }

  def toJSON (c: NodeComment): JValue = {
    ("type" -> "comments") ~
    ("id" -> c.hash.get) ~
    ("children" -> NodeComment.toJSON(c.children)) ~
    ("attributes" ->
      ("parent" -> c.parentHash.get) ~
      ("text" -> c.text.get) ~
      ("upvotes" -> c.upVotes.get) ~
      ("downvotes" -> c.downVotes.get) ~
      ("uservote" -> User.currentUser.map(_.votedForComment(c).map(_.vote.get) openOr 0).openOr(0)) ~
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