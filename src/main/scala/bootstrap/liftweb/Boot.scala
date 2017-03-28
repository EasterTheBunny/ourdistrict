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

package bootstrap.liftweb

import net.liftweb._
import util.Helpers._
import util.Props
import common._
import http._
import sitemap._
import Loc._
import mapper._
import net.liftweb.db.DBLogEntry
import net.liftmodules.FoBo
import code.model._
import code.lib._
import code.snippet.{AllTopicsPage, ChartsPage, TopicPage, DocumentPage}
import scravatar.{DefaultImage, Gravatar}
import java.sql._

object PrimaryDBVender extends ConnectionManager {
  private var pool: List[Connection] = Nil
  private var poolSize = 0
  private val maxPoolSize = 4
 
  private lazy val chooseDriver = Props.get("db.driver") openOr "org.h2.Driver"
  private lazy val chooseURL = Props.get("db.url") openOr "jdbc:h2:mem:ourdistrict_demo;DB_CLOSE_DELAY=-1"

  private def createOne: Box[Connection] = try {
    val driverName: String = chooseDriver
    val dbUrl: String = chooseURL
 
    Class.forName(driverName)
 
    val dm = (Props.get("db.user"), Props.get("db.password")) match {
      case (Full(user), Full(pwd)) =>
        DriverManager.getConnection(dbUrl, user, pwd)
 
      case _ => DriverManager.getConnection(dbUrl)
    }
 
    Full(dm)
  } catch {
    case e: Exception => e.printStackTrace; Empty
  }
 
  def newConnection(name: ConnectionIdentifier): Box[Connection] =
    synchronized {
      pool match {
        case Nil if poolSize < maxPoolSize =>
          val ret = createOne
          poolSize = poolSize + 1
          ret.foreach(c => pool = c :: pool)
          ret
 
        case Nil => wait(1000L); newConnection(name)
        case x :: xs => try {
          x.setAutoCommit(false)
          Full(x)
        } catch {
          case e => try {
            pool = xs
            poolSize = poolSize - 1
            x.close
            newConnection(name)
          } catch {
            case e => newConnection(name)
          }
        }
      }
    }
 
  def releaseConnection(conn: Connection): Unit = synchronized {
    pool = conn :: pool
    notify
  }
  
}

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    val logger = Logger(classOf[Boot])

    logger.debug("testing")

    DB.defineConnectionManager(DefaultConnectionIdentifier, PrimaryDBVender)
    DB.addLogFunc {
      case (log, duration) => {
        //println("Total query time : %d ms".format(duration))
        log.allEntries.foreach {
          case DBLogEntry(stmt, duration) =>
            //println("  %s in %d ms".format(stmt, duration))
        }
      }
    }

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, User, Node, Topic, Comment, Soapbox, Bill, BillSponsor,
    							Committee, CommitteeBill, CommitteeTopic, CommitteeUser, Sponsor, Action, Visualization, SortedTopic, SortedTopicCookie)

    // where to search snippet
    LiftRules.addToPackages("code")

    def sitemapMutators = User.sitemapMutator andThen
                          Topic.sitemapMutator andThen
                          Soapbox.sitemapMutator andThen
                          Visualization.sitemapMutator

    //The SiteMap is built in the Site object bellow 
    LiftRules.setSiteMapFunc(() => sitemapMutators(Site.sitemap))

    //Init the FoBo - Front-End Toolkit module, 
    //see http://liftweb.net/lift_modules for more info
    FoBo.Toolkit.Init=FoBo.Toolkit.JQuery310 //JQuery1113
    FoBo.Toolkit.Init=FoBo.Toolkit.Bootstrap337
    FoBo.Toolkit.Init=FoBo.Toolkit.FontAwesome463

    //LiftRules.explicitlyParsedSuffixes += "pdf"
    
    LiftRules.statelessDispatch.append(PDFServer)
    LiftRules.dispatch.append(JsonNodeHandler)
    LiftRules.dispatch.append(JsonDataHandler)
    LiftRules.dispatch.append(JsonUserSortedListHandler)

    Props.get("settings.fullnode") match {
      case Full(opt) if(opt == "true") => {
        //ParseBillsToDB ! ParseBillsToDB.ParseBills
        ParseLegislatorsToDB ! ParseLegislatorsToDB.ParseLegislators

        LiftRules.unloadHooks.append( () => ParseBillsToDB ! ParseBillsToDB.Stop )
        LiftRules.unloadHooks.append( () => ParseLegislatorsToDB ! ParseLegislatorsToDB.Stop )
      }
      case _ => {
        println("You have opted to NOT run a full node")
      }
    }

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    
      
    LiftRules.noticesAutoFadeOut.default.set( (notices: NoticeType.Value) => {
        notices match {
          case NoticeType.Notice => Full((3 seconds, 2 seconds))
          case NoticeType.Warning => Full((5 seconds, 2 seconds))
          case NoticeType.Error => Full((10 seconds, 2 seconds))
          case _ => Empty
        }
     }
    )
    
    //Lift CSP settings see http://content-security-policy.com/ and 
    //Lift API for more information.  
    LiftRules.securityRules = () => {
      SecurityRules(content = Some(ContentSecurityPolicy(           
        scriptSources = List(
            ContentSourceRestriction.Self,
            ContentSourceRestriction.UnsafeInline,
            ContentSourceRestriction.UnsafeEval,
            ContentSourceRestriction.Host("https://code.jquery.com"),
            ContentSourceRestriction.Host("http://code.jquery.com"),
            ContentSourceRestriction.Host("https://maxcdn.bootstrapcdn.com"),
            ContentSourceRestriction.Host("http://d3js.org")),   
        styleSources = List(
            ContentSourceRestriction.Self,
            ContentSourceRestriction.UnsafeInline,
            ContentSourceRestriction.Host("https://code.jquery.com"),
            ContentSourceRestriction.Host("https://maxcdn.bootstrapcdn.com"),
            ContentSourceRestriction.Host("http://code.ionicframework.com"),
            ContentSourceRestriction.Host("https://fonts.googleapis.com")),
        fontSources = List(
            ContentSourceRestriction.Self,
            ContentSourceRestriction.Host("https://maxcdn.bootstrapcdn.com"),
            ContentSourceRestriction.Host("http://code.ionicframework.com"),
            ContentSourceRestriction.Host("https://fonts.googleapis.com"),
            ContentSourceRestriction.Host("https://fonts.gstatic.com"))
            )))
    }
    
    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

  }

  object Site {
    import scala.xml._
    //if user is logged in replace menu label "User" with users gravatar image and full name.
    def userDDLabel: NodeSeq = { 
      /*def gravatar: NodeSeq = {
        val gurl = Gravatar(User.currentUser.map(u => u.email.get).openOrThrowException("")).size(36).avatarUrl
        <img class="responsive-img img-rounded gravatar" src={gurl}/> 
      }*/     
      lazy val username = User.currentUser.map(u => u.username)
      User.loggedIn_? match {
        case true =>  <xml:group>{username.openOrThrowException("Something wicked happened")}</xml:group> 
        case _ => <xml:group><span class="glyphicon glyphicon-user" aria-hidden="true"></span></xml:group>   
      }
    }

    val ddLabel1   = Menu(userDDLabel) / "ddlabel1"
    val divider1   = Menu("divider1") / "divider1"
    val home       = Menu.i("Home") / "index"
    val thepark    = Menu(Loc("The Park", "thepark" :: Nil, "The Park", LocGroup("site", "navbar")))
    
    val userMenu   = User.AddUserMenusHere
     
    val FLTDemo       = Menu(Loc("FLTDemo", 
        ExtLink("http://www.media4u101.se/fobo-lift-template-demo/"), 
        S.loc("FLTDemo" , scala.xml.Text("FoBo Lift Template Demo")),
        LocGroup("lg2")/*,
        FoBo.TBLocInfo.LinkTargetBlank */ ))               
        
    def sitemap = SiteMap(
        home          >> LocGroup("site", "navbar"),
        thepark,
        ChartsPage.menu,
        AllTopicsPage.menu,
        DocumentPage.menu >> Hidden,
        TopicPage.menu >> Hidden >> Topic.AddAccountMenusAfter >> Soapbox.AddSoapboxMenusAfter,
        ddLabel1      >> LocGroup("topRight") >> PlaceHolder submenus (
            divider1  /*>> FoBo.TBLocInfo.Divider*/ >> userMenu
            )
         )
  }

}
