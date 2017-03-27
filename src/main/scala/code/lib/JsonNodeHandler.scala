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
package lib

import model._
import net.liftweb._
import mapper._
import util._
import common._
import TimeHelpers._
import http._
import rest._
import json._

import net.liftweb.json.JsonDSL._
import net.liftweb.json.JsonAST._

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat


object JsonNodeHandler extends RestHelper {
  
  def toJSON (n: Node): JValue = {
    /*"node" ->*/
    	("statement" -> n.statement.get) ~
    	("details" -> n.details.get) ~
    	("vote" -> n.vote.get) ~
    	("uservote" -> n.voteForCurrentUser_?) ~
    	("date" -> DateTimeFormat.forPattern("MMMM e, yyyy HH:mm:ss").print(new DateTime(n.dateCreated.get))) ~
    	("parent" -> n.parentHash.get) ~
    	("version" -> n.version.get) ~
    	("id" -> n.hash.get) ~
    	("user" -> n.creator.obj.map(_.username.get).openOr("unknown")) ~
      ("children" -> n.children.map(toJSON(_)))
  }

  def toJSON (n: List[Node]): JValue = {
    n.map(toJSON(_))
  }
  
  def toCommentJSON (c: Comment): JValue = {
    "comment" ->
      ("id" -> c.hash.get) ~
      ("parent" -> c.parentHash.get) ~
      ("vote" -> c.vote.get) ~
      ("text" -> c.text.get) ~
      ("uservote" -> c.voteForCurrentUser_?) ~
      ("user" -> c.creator.obj.map(_.username.get).openOr("unknown"))
    
  }
  
  def toCommentJSON (c: List[Comment]): JValue = {
    c.map(comment => {
      ("id" -> comment.hash.get) ~
       ("parent" -> comment.parentHash.get) ~
       ("vote" -> comment.vote.get) ~
       ("text" -> comment.text.get) ~
       ("uservote" -> comment.voteForCurrentUser_?) ~
       ("user" -> comment.creator.obj.map(_.username.get).openOr("unknown"))
    })
  }
  
  serve( "json" / "vote" prefix {
    case "node" :: nodeHash :: _ Post _ if !User.currentUser.isEmpty =>
      for {
        vote <- S.param("vote") ?~ "vote parameter missing" ~> 400
        node <- Node.find(By(Node.hash, nodeHash)) ?~ "node not found"
      } yield {
        if(vote.equals("up")) {
          if(node.voteForCurrentUser_? <= 0) {
            val voteVal = node.voteForCurrentUser_?
            node.voteUpForUser(User.currentUser.openOrThrowException("accessing /json/vote/node/xxx - current user not found"))
            node.vote(node.vote.get + (voteVal * -1) + 1).save
          }
          //node.voteUpForUser(User.currentUser.open_!)
          //node.vote(node.vote.get + (node.voteForCurrentUser_? * -1) + 1).save
        }
        else if(vote.equals("down")) {
          if(node.voteForCurrentUser_? >= 0) {
        	val voteVal = node.voteForCurrentUser_?
        	node.voteDownForUser(User.currentUser.openOrThrowException("accessing /json/vote/node/xxx - current user not found"))
        	node.vote(node.vote.get + (voteVal * -1) - 1).save
          }
        }
        if(true){
          Full(ForbiddenResponse())
        }
        toJSON(node)
      }
    case "comment" :: commentHash :: _ Post _ if !User.currentUser.isEmpty =>
      for {
        vote <- S.param("vote") ?~ "vote parameter missing" ~> 400
        comment <- Comment.find(By(Comment.hash, commentHash)) ?~ "comment not found"
      } yield {
        val userVote = comment.voteForCurrentUser_?
        if(vote.equals("up") && userVote <= 0) {
          comment.voteUpForUser(User.currentUser.openOrThrowException("accessing /json/vote/comment/xxx - current user not found"))
          comment.vote(comment.vote.get + (userVote * -1) + 1).save
        }
        else if(vote.equals("down") && userVote >= 0) {
          comment.voteDownForUser(User.currentUser.openOrThrowException("accessing /json/vote/comment/xxx - current user not found"))
          comment.vote(comment.vote.get + (userVote * -1) - 1).save
        }
        toCommentJSON(comment)
      }
  })
  
  serve( "json" / "add" prefix {
    case "node" :: _ Post _ if !User.currentUser.isEmpty =>
      for {
        currentNodeHash <- S.param("current-node-id") ?~ "node parameter missing" ~> 400
        statement <- S.param("new-node-statement") ?~ "statement parameter missing"
        details <- S.param("new-node-details") ?~ "details parameter missing"
        topicHash <- S.param("current-topic-id") ?~ "topic parameter missing"
      } yield {
        val node = Node.add(statement, details, topicHash, currentNodeHash)
        toJSON(node)
      }
      
    case "version" :: _ Post _ if !User.currentUser.isEmpty =>
      for {
        currentNodeHash <- S.param("current-node-id") ?~ "node parameter missing" ~> 400
        statement <- S.param("version-node-statement") ?~ "statement parameter missing"
        details <- S.param("version-node-details") ?~ "details parameter missing"
        topicHash <- S.param("current-topic-id") ?~ "topic parameter missing"
      } yield {
        // validate this
        val node = Node.addVersion(statement, details, topicHash, currentNodeHash)
        toJSON(node)
      }
      
    case "comment" :: _ Post _ if !User.currentUser.isEmpty =>
      for {
        text <- S.param("new-comment-text") ?~ "text parameter missing" ~> 400
        nodeHash <- S.param("current-node-id") ?~ "node parameter missing"
        version <- S.param("current-node-version") ?~ "version parameter missing"
        topicHash <- S.param("current-topic-id") ?~ "topic parameter missing"
        parentHash <- S.param("current-comment-id") ?~ "parent parameter missing"
        comment <- Comment.add(text, nodeHash, version.toInt, topicHash, parentHash) ?~ "internal error"
      } yield {
        toCommentJSON(comment)
      }
  })
  
  serve {
    
    case "json" :: "topic" :: topicHash :: Nil JsonGet _ =>
      for {
        topic <- Topic.find(By(Topic.hash, topicHash)) ?~ "Topic not found"
      } yield {
        val totalNodeVotes = topic.nodes.map(_.vote.get).foldLeft(0)((b, a) => {
          a + b
        })
        // is this saving data to the db EVERY time it is accessed????
        // what was I thinking???!!?
        topic.nodeVoteTotal(totalNodeVotes).save

        /**
          * we need to structure this as a nested tree for easy consumption
          * the client side
          */
        val nodes = topic.nodes

        // set the hash lookup
        val lookup = nodes.foldLeft(Map[String, Node]())((mp, li) => {
          mp + (li.hash.get -> li)
        })

        // collapse the nodes
        var roots: List[Node] = Nil
        nodes.foreach(li => {
          if(lookup.contains(li.parentHash.get)) {
            lookup(li.parentHash.get) children = lookup(li.parentHash.get).children :+ li
          }

          if(li.parentHash.get == "root") roots = roots :+ li
        })

        toJSON(roots)
      }
    
    case "json" :: "node" :: nodeHash :: Nil JsonGet _ => 
      for {
        node <- Node.find(By(Node.hash, nodeHash)) ?~ "Node not found"
      } yield toJSON(node)
      
    case "json" :: "comments" :: topicHash :: nodeHash :: versionid :: Nil JsonGet _ =>
      for {
        topic <- Topic.searchByHash(topicHash) ?~ "Topic not found"
        node <- Node.find(By(Node.hash, nodeHash), By(Node.version, versionid.toInt), By(Node.topic, topic)) ?~ "Node not found"
        //comments <- Comment.findTopLevelCommentsForNode(nodeId.toLong) ?~ "Comment not found"
      } yield {
        val comments = Comment.findCommentsForNode(node)
        val totalCommentVotes = comments.map(_.vote.get).foldLeft(0)((b, a) => a + b)
        node.commentVoteTotal(totalCommentVotes).save
        toCommentJSON(comments)
      }
      
    case "json" :: "comment" :: commentHash :: Nil JsonGet _ =>
      for {
        comment <- Comment.find(By(Comment.hash, commentHash))
      } yield toCommentJSON(comment)
      
  }
  
}