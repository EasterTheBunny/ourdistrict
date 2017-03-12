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

class Soapbox extends LongKeyedMapper[Soapbox] with IdPK {
  def getSingleton = Soapbox
  object creator extends MappedLongForeignKey(this, User) {
    override def dbIndexed_? = true
  }
  object dateCreated extends MappedDateTime(this)
  object hash extends MappedString(this, 72) {
    override val defaultValue = randomString(maxLen)
  }
  object title extends MappedString(this, 140)
  object body extends MappedString(this, 2000)
  
}

object Soapbox extends Soapbox with LongKeyedMetaMapper[Soapbox] {
  override def dbTableName = "soapbox"
    
  def screenWrap = Full(<div id="main" class="lift:surround?with=minimal;at=content"><div class="container-fluid"><div class="row"><div class="col-md-12"><div class="col-centered" style="width:500px;"><lift:bind /></div></div></div></div></div>)
  
  lazy val testLoggedIn = If(User.loggedIn_? _, S.?("must.be.logged.in"))
  
  val addSoapboxPath = "soapbox" :: Nil
    
  def addSoapboxMenuLoc: Box[Menu] =
    Full(Menu(Loc("Add Speech", addSoapboxPath, "Grab Your Soapbox", addSoapboxMenuLocParams)))
    
  protected def addSoapboxMenuLocParams: List[LocParam[Unit]] = 
    Template(() => wrapIt(addSoapbox)) ::
    testLoggedIn ::
    LocGroup("soapbox") ::
    Nil
    
  final case object AddSoapboxMenusAfter extends Loc.LocParam[Any]
  final case object AddSoapboxMenusHere extends Loc.LocParam[Any]
  final case object AddSoapboxMenusUnder extends Loc.LocParam[Any]
  
  private lazy val AfterUnapply = SiteMap.buildMenuMatcher(_ == AddSoapboxMenusAfter)
  private lazy val HereUnapply = SiteMap.buildMenuMatcher(_ == AddSoapboxMenusHere)
  private lazy val UnderUnapply = SiteMap.buildMenuMatcher(_ == AddSoapboxMenusUnder)
  
  def sitemapMutator: SiteMap => SiteMap = SiteMap.sitemapMutator {
    case AfterUnapply(menu) => menu :: sitemap
    case HereUnapply(_) => sitemap
    case UnderUnapply(menu) => List(menu.rebuild(_ ::: sitemap))
  }(SiteMap.addMenusAtEndMutator(sitemap))
  
  lazy val sitemap: List[Menu] = List(addSoapboxMenuLoc).flatten(a => a)
  
  def addSoapbox = {
    var soapboxTitle = ""
    var soapboxBody = ""
    
    def testAndSet() {
      val user = User.currentUserLog
      
      if(true){
        val newSoapbox = Soapbox.create
        newSoapbox.title(soapboxTitle).body(soapboxBody).dateCreated(new java.util.Date()).creator(user)
        
        newSoapbox.validate match {
          case Nil => {
             newSoapbox.save
             
             S.notice("new soapbox created")
             S.redirectTo("/thepark")
          }
          case xs => S.error(xs)
        }
      }
    }
    
    val bind = 	"@title" #> SHtml.text("", s => soapboxTitle = s.trim) &
    			"@body" #> SHtml.textarea("", s => soapboxBody = s.trim) &
    			"type=submit" #> SHtml.submit(S.?("Listen To Me!!"), testAndSet _)
    			
    bind(addTopicXhtml)
  }
  
  private lazy val newSoapboxInstructions = "Preach it to the world. Whatever you have to say, say it here. Visitors will have 4 hours to read your soapbox speech before it's lost to the nethernet."
 
  def addTopicXhtml = {
    (<form method="post" action={S.uri}>
    	<table class="table table-striped"><thead><tr><td><h3>Grab a Soapbox</h3></td></tr></thead>
    		<tbody>
    			<tr><td><h4>Speak your mind</h4></td></tr>
    			<tr><td>
    				<div class="form-group">
    					<label for="soapbox-title">Title</label>
    					<input id="soapbox-title" type="text" name="title" class="form-control" placeholder="A Sexy Title" />
    				</div>
    			</td></tr>
    			<tr><td>
    				<div class="form-group">
    					<label for="soapbox-body">Your Word</label>
    					<textarea id="soapbox-body" name="body" class="form-control" rows="5" placeholder={ newSoapboxInstructions } />
    				</div>
    			</td></tr>
    			<tr><td><input type="submit" class="btn btn-primary pull-right" /></td></tr>
    		</tbody>
    	</table></form>)
  }
  
  def speeches = {
    
    
  }
  
  def speechXhtml ={
    
    
  }
    
  protected def wrapIt(in: NodeSeq): NodeSeq =
    screenWrap.map(new RuleTransformer(new RewriteRule {
      override def transform(n: scala.xml.Node) = n match {
        case e: Elem if "bind" == e.label && "lift" == e.prefix => in
        case _ => n
      }
    })) openOr in

}