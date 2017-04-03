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

import code.mapper.MappedList

import com.roundeights.hasher.Implicits._

class Comment extends LongKeyedMapper[Comment] with IdPK {
  def getSingleton = Comment
  
  object text extends MappedString(this, 1000)
  object dateCreated extends MappedDateTime(this)
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object node extends MappedLongForeignKey(this, Node)
  object hash extends MappedString(this, 64)
  object vote extends MappedInt(this)
  object upvotes extends MappedList(this) {
    override def defaultValue = "[]"
  }
  object downvotes extends MappedList(this) {
    override def defaultValue = "[]"
  }
  object topic extends MappedLongForeignKey(this, Topic)
  object parent extends MappedLong(this) {
    override def dbIndexed_? = true
  }
  object parentHash extends MappedString(this, 64)
  
  def children: List[Comment] = {
    Comment.findAll(By(Comment.parent, this.id.get))
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
}

object Comment extends Comment with LongKeyedMetaMapper[Comment] {
  override def dbTableName = "comment"
    
  def findCommentsForNode(node: Node): List[Comment] = {
    Comment.findAll(By(Comment.node, node.id.get), OrderBy(Comment.vote, Descending), MaxRows(300))
  }
  
  def findTopLevelCommentsForNode(node: Node): List[Comment] = {
    Comment.findAll(By(Comment.node, node.id.get), By(Comment.parent, 0))
  }
  
  def add(text: String, nodeHash: String, version: Int, topicHash: String, parentHash: String): Box[Comment] = {
    for {
      topic <- Topic.find(By(Topic.hash, topicHash)) ?~ "topic not found"
      node <- Node.find(By(Node.hash, nodeHash), By(Node.version, version), By(Node.topic, topic)) ?~ "node not found"
    } yield {
      val comment = Comment.create.text(text.trim()).dateCreated(new java.util.Date()).node(node).topic(topic).parentHash(parentHash).vote(1).creator(User.currentUserLog)
      comment.save
      topic.commentCount(topic.commentCount.get + 1).save
      node.commentCount(node.commentCount.get + 1).save
      val hash = (comment.id.get.toString + comment.text.get).crc32
      comment.upvotes.add(User.currentUser.map(_.id.get.toString).openOrThrowException("")).save
      comment.hash(hash).save
      comment
    }
  }
  
}