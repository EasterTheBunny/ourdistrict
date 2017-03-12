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

import code.mapper.MappedList

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.format.DateTimeFormatter

import com.roundeights.hasher.Implicits._

class Node extends LongKeyedMapper[Node] with IdPK {
  def getSingleton = Node
  
  object statement extends MappedString(this, 500)
  object details extends MappedString(this, 2000)
  object dateCreated extends MappedDateTime(this)
  object vote extends MappedInt(this)
  object nodeID extends MappedInt(this)
  object hash extends MappedString(this, 64)
  object version extends MappedInt(this)
  object upvotes extends MappedList(this, 3000) {
    override def defaultValue = "[]"
  }
  object downvotes extends MappedList(this, 3000) {
    override def defaultValue = "[]"
  }
  object commentCount extends MappedInt(this)
  object commentVoteTotal extends MappedInt(this)
  object topic extends MappedLongForeignKey(this, Topic) {
    override def dbIndexed_? = true
  }
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object parent extends MappedInt(this) {
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
  
}

object Node extends Node with LongKeyedMetaMapper[Node] {
  override def dbTableName = "node"
    
  override def unapply(a: Any): Option[Node] = Node.find(By(Node.hash, a.toString))
    
  def findById(id: Int): Box[Node] = {
    for {
      node <- Node.find(By(Node.nodeID, id)) ?~ "Node not found"
    } yield node
  }
  
  def add(statement: String, details: String, topicHash: String, parentHash: String): Node = {
    User.currentUser match {
      case Full(user) => {
        
      }
      case _ => Empty
    }
    val topic = Topic.searchByHash(topicHash)
    
    val newnode = Node.create.statement(statement).topic(topic).parentHash(parentHash).dateCreated(new java.util.Date()).creator(User.currentUserLog).details(details).version(0).vote(1)
    newnode.upvotes.add(User.currentUser.map(_.id.get.toString).openOrThrowException("")).save
    topic.map(a => a.nodeCount(a.nodeCount.get + 1).save)
    newnode.save
    val hash = (newnode.id.get.toString + newnode.statement.get).crc32
    newnode.hash(hash).save
    newnode
  }
  
  def addVersion(statement: String, details: String, topicHash: String, nodeHash: String): Node = {
    var parent = ""
    var version = 0
    val topic = Topic.searchByHash(topicHash)
    
    Node.findAll(By(Node.topic, topic), By(Node.hash, nodeHash)).map(nd => {
      parent = nd.parentHash.get
      if(nd.version.get > version) version = nd.version.get
    })
    val newnode = Node.create.statement(statement).topic(topic).parentHash(parent).dateCreated(new java.util.Date()).creator(User.currentUserLog).details(details).version(version + 1).vote(1).hash(nodeHash)
    newnode.upvotes.add(User.currentUser.map(_.id.get.toString).openOrThrowException("")).save
    topic.map(a => a.nodeCount(a.nodeCount.get + 1).save)
    newnode.save
    newnode
  }
}