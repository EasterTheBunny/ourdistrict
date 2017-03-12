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
import net.liftweb.sitemap._
import Helpers._
import Loc._
import js._
import JsCmds._

import scala.xml.{NodeSeq,Elem}
import scala.xml.transform._

import com.roundeights.hasher.Implicits._

class Topic extends LongKeyedMapper[Topic] with IdPK {
  def getSingleton = Topic
  
  object title extends MappedString(this, 200)
  object hash extends MappedString(this, 64)
  object details extends MappedString(this, 1000)
  object dateCreated extends MappedDateTime(this)
  object nodeCount extends MappedInt(this)
  object commentCount extends MappedInt(this)
  object nodeVoteTotal extends MappedInt(this)
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  
  def nodes: List[Node] = {
    Node.findAll(By(Node.topic, this.id.get))
  }
  
  def comments: List[Comment] = {
    Comment.findAll(By(Comment.topic, this.id.get))
  }
  
}

object Topic extends Topic with LongKeyedMetaMapper[Topic] {
  override def dbTableName = "topic"
    
  override def unapply(a: Any): Option[Topic] = Topic.find(By(Topic.hash, a.toString))
    
  def searchByHash(hash: String): Box[Topic] = {
    try{
      Topic.find(By(Topic.hash, hash))
    } catch {
      case e: Exception => Empty
    }
  }
  
  def screenWrap = Full(<div id="main" class="lift:surround?with=minimal;at=content"><div class="container-fluid"><div class="row"><div class="col-md-12"><div class="col-centered" style="width:500px;"><lift:bind /></div></div></div></div></div>)

  lazy val testLoggedIn = If(User.loggedIn_? _, S.?("must.be.logged.in"))
  
  val addTopicPath = "addtopic" :: Nil
    
  def addTopicMenuLoc: Box[Menu] =
    Full(Menu(Loc("Add Topic", addTopicPath, "Add Topic", addTopicMenuLocParams)))
    
  protected def addTopicMenuLocParams: List[LocParam[Unit]] = 
    Template(() => wrapIt(addTopic)) ::
    testLoggedIn ::
    LocGroup("admin") ::
    Nil
    
  final case object AddAccountMenusAfter extends Loc.LocParam[Any]
  final case object AddAccountMenusHere extends Loc.LocParam[Any]
  final case object AddAccountMenusUnder extends Loc.LocParam[Any]
  
  private lazy val AfterUnapply = SiteMap.buildMenuMatcher(_ == AddAccountMenusAfter)
  private lazy val HereUnapply = SiteMap.buildMenuMatcher(_ == AddAccountMenusHere)
  private lazy val UnderUnapply = SiteMap.buildMenuMatcher(_ == AddAccountMenusUnder)
  
  def sitemapMutator: SiteMap => SiteMap = SiteMap.sitemapMutator {
    case AfterUnapply(menu) => menu :: sitemap
    case HereUnapply(_) => sitemap
    case UnderUnapply(menu) => List(menu.rebuild(_ ::: sitemap))
  }(SiteMap.addMenusAtEndMutator(sitemap))
  
  lazy val sitemap: List[Menu] = List(addTopicMenuLoc).flatten(a => a)
  
  def addTopic = {
    var topicTitle = ""
    var topicDetails = ""
    var firstNodeTitle = ""
    var firstNodeDetails = ""
    
    def testAndSet() {
      val user = User.currentUserLog
      
      if(true){
        val newTopic = Topic.create
        newTopic.title(topicTitle).details(topicDetails).dateCreated(new java.util.Date()).creator(user)
        
        val newNode = Node.create
        newNode.statement(firstNodeTitle).details(firstNodeDetails).creator(user).dateCreated(new java.util.Date()).version(0).parent(0).vote(1).parentHash("root")
        
        
        newTopic.validate match {
          case Nil => {
            
            newNode.validate match {
              case Nil => {
                newTopic.save
                val hash = (newTopic.id.get.toString + newTopic.title.get).crc32
                newTopic.hash(hash.hex)
                newTopic.save
                
                newNode.topic(newTopic).save
                val nodeHash = (newNode.id.get.toString + newNode.statement.get).crc32
                newNode.hash(nodeHash.hex)
                newNode.save
                
                S.notice("new topic added"); S.redirectTo("/topic/" + newTopic.hash.get)
              }
              case xs => S.error(xs)
            }
          }
          case xs => S.error(xs)
        }
      }
    }
    
    val bind = 	"@title" #> SHtml.text("", s => topicTitle = s.trim) &
    			"@details" #> SHtml.textarea("", s => topicDetails = s.trim) &
    			"@node-title" #> SHtml.text("", s => firstNodeTitle = s.trim) &
    			"@node-details" #> SHtml.textarea("", s => firstNodeDetails = s.trim) &
    			"type=submit" #> SHtml.submit(S.?("save"), testAndSet _)
    			
    bind(addTopicXhtml)
  }
  
  private lazy val newTopicInstructions = "Write a general goal of the topic here. Describe the concepts, but don't go into detail on propositions."
  private lazy val newNodeInstruction = "Write an initial proposition for your topic here. Give enough detail to establish direction, but allow for branching of ideas. Consider this to be an introduction to a legal document."
  
  def addTopicXhtml = {
    (<form method="post" action={S.uri}>
    	<table class="table table-striped"><thead><tr><td><h3>Add Topic</h3></td></tr></thead>
    		<tbody>
    			<tr><td><h4>New Topic Details</h4></td></tr>
    			<tr><td>
    				<div class="form-group">
    					<label for="topic-title">Title</label>
    					<input id="topic-title" type="text" name="title" class="form-control" placeholder="New Topic Title" />
    				</div>
    			</td></tr>
    			<tr><td>
    				<div class="form-group">
    					<label for="topic-details">Details</label>
    					<textarea id="topic-details" name="details" class="form-control" rows="5" placeholder={ newTopicInstructions } />
    				</div>
    			</td></tr>
    			<tr><td><h4>First Node Details</h4></td></tr>
    			<tr><td>
    				<div class="form-group">
    					<label for="node-title">Title</label>
    					<input id="node-title" type="text" name="node-title" class="form-control" placeholder="" />
    				</div>
    			</td></tr>
    			<tr><td>
    				<div class="form-group">
    					<label for="node-details">Details</label>
    					<textarea id="node-details" name="node-details" class="form-control" rows="5" placeholder={ newNodeInstruction } />
    				</div>
    			</td></tr>
    			<tr><td><input type="submit" class="btn btn-primary pull-right" /></td></tr>
    		</tbody>
    	</table></form>)
  }
    
  protected def wrapIt(in: NodeSeq): NodeSeq =
    screenWrap.map(new RuleTransformer(new RewriteRule {
      override def transform(n: scala.xml.Node) = n match {
        case e: Elem if "bind" == e.label && "lift" == e.prefix => in
        case _ => n
      }
    })) openOr in
}