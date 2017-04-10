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

import net.liftweb.mapper.{By, _}
import net.liftweb.util._
import Helpers._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.sitemap._
import Loc._
import js._
import JsCmds._
import net.liftmodules.FoBoBs.mapper._
import net.liftweb.json._

import scala.xml.{Attribute, Elem, MetaData, NodeSeq, Text}
import code.mapper.MappedList

/**
 * The singleton that has methods for accessing the database
 */
object User extends User with MetaMegaProtoUser[User] with BootstrapMegaMetaProtoUser[User] {
  override def dbTableName = "users" // define the DB table name
  // define the order fields will appear in forms and output
  override def fieldOrder = List(id, firstName, lastName, email,
  locale, timezone, password)

  // comment this line out to require email validations
  override def skipEmailValidation = true
  
  def currentUserLog = {
    super.currentUser match {
    	case Full(user) => {
    	  val list = user.ipaccesses.asList
    	  val ip = S.containerRequest match {
    	    case Full(r) => r.header("X-Forwarded-For").openOr("127.0.0.1")
    	    case _ => "127.0.0.1"
    	  }
    	  if(ip.length() > 0 && !list.contains(ip)) user.ipaccesses(list :+ ip).lastAccess(new java.util.Date()).save
    	}
    }
    super.currentUser
  }
  
  override val basePath: List[String] = "user" :: Nil
  override protected def globalUserLocParams: List[LocParam[Unit]] = LocGroup("usermgmt") :: Nil
  override def validateUserMenuLoc: Box[Menu] = Empty
  override def screenWrap = Full(<div id="main" class="lift:surround?with=minimal;at=content"><div class="container" style="padding-top:30px;"><div class="row"><div class="col-md-6 col-md-offset-3 col-sm-12"><lift:bind /></div></div></div></div>)
  
  override def signupFields = List(username, email, password)

  override def loginXhtml = {
    //val lpwPath = if (S.contextPath != "") S.contextPath + lostPasswordPath.mkString("/", "/", "") else lostPasswordPath.mkString("/", "/", "")
    <form class="form-horizontal" role="form" action={ S.uri } method="post">
      <legend>{ resLoginLegendLogin }</legend>
      <div class="form-group">
        <label for="username" class="col-lg-3 control-label">{ userNameFieldString }</label>
        <div class="col-lg-9">
          <input type="email" class="form-control" id="username" name="username" placeholder={ userNameFieldString } autofocus="autofocus"/>
        </div>
      </div>
      <div class="form-group">
        <label for="password" class="col-lg-3 control-label">{ resLoginLabelPassword }</label>
        <div class="col-lg-9">
          <input type="password" class="form-control" id="password" name="password" placeholder={ resLoginPlaceholderPassword }/>
        </div>
      </div>
      <div class="form-group">
        <div class="col-lg-offset-3 col-lg-10">
          <button type="submit" class="btn btn-primary">{ resLoginSubmit }</button><span> <a href={ lostPasswordPath.mkString("/", "/", "") }>{ resLoginLabelRecoverPassword }</a></span>
        </div>
      </div>
    </form>
  }

  override def lostPasswordXhtml = {
    <form class="form-horizontal" role="form" action={ S.uri } method="post">
       <legend>{ resLostPasswordLegendEnterEmail }</legend>
       <div class="form-group">
         <label for="username" class="col-lg-3 control-label">{ resLostPasswordLabelUserName }</label>
         <div class="col-lg-9">
           <input type="email" class="form-control" id="username" name="username" placeholder={ resLostPasswordPlaceholderUserName } autofocus="autofocus"/>
         </div>
       </div>
       <div class="form-group">
         <div class="col-lg-offset-3 col-lg-10">
           <button type="submit" class="btn btn-primary">{ resLostPasswordSubmit }</button>
         </div>
       </div>
     </form>
  }

  override def changePasswordXhtml = {
    <form class="form-horizontal" role="form" method="post" action={ S.uri }>
       <legend>{ resChangePasswordLegendChangePassword }</legend>
       <div class="form-group">
         <label for="oldpassword" class="col-lg-3 control-label">{ resChangePasswordLabelOldPassword }</label>
         <div class="col-lg-9">
           <input type="password" class="old-password form-control" placeholder={resChangePasswordPlaceholderOldPassword} />
         </div>
       </div>
       <div class="form-group">
         <label for="newpassword" class="col-lg-3 control-label">{ resChangePasswordLabelNewPassword }</label>
         <div class="col-lg-9">
           <input type="password" class="new-password form-control" placeholder={resChangePasswordPlaceholderNewPassword}/>
         </div>
       </div>
       <div class="form-group">
         <label for="repeatpassword" class="col-lg-3 control-label">{ resChangePasswordLabelRepeatPassword }</label>
         <div class="col-lg-9">
           <input type="password" class="new-password form-control" placeholder={resChangePasswordPlaceholderNewPassword}/>
         </div>
       </div>
       <div class="form-group">
         <div class="col-lg-offset-3 col-lg-10">
           <input type="submit" class="btn btn-primary"/>
         </div>
       </div>
     </form>
  }

  override def edit = {
    val theUser: TheUserType = mutateUserOnEdit(
      currentUser.openOrThrowException("we know we're logged in"))
    val theName = editPath.mkString("")
    val submitAttr: Seq[SHtml.ElemAttr] = Seq("class" -> "btn btn-primary")

    def testEdit() {
      theUser.validate match {
        case Nil =>
          theUser.save
          S.notice(S.?("profile.updated"))
          S.redirectTo(homePage)

        case xs => S.error(xs); editFunc(Full(innerEdit _))
      }
    }

    def innerEdit = {
      ("type=submit" #> editSubmitButton(resEditSubmitSave,
                                         testEdit _,
                                         submitAttr: _*)) apply editXhtml(
        theUser)
    }

    innerEdit
  }

  override def signup = {
    val theUser: TheUserType = mutateUserOnSignup(createNewUserInstance())
    val theName = signUpPath.mkString("")
    val submitAttr: Seq[SHtml.ElemAttr] = Seq("class" -> "btn btn-primary")

    def testSignup() {
      validateSignup(theUser) match {
        case Nil =>
          actionsAfterSignup(theUser, () => S.redirectTo(homePage))

        case xs => S.error(xs); signupFunc(Full(innerSignup _))
      }
    }

    def innerSignup = {
      ("type=submit" #> signupSubmitButton(resSignUpSubmitSignUp,
                                           testSignup _,
                                           submitAttr: _*)) apply signupXhtml(
        theUser)
    }
    innerSignup
  }
  
  override def editFields: List[FieldPointerType] = List(firstName,
                                                         lastName,
                                                         email,
                                                         locale,
                                                         timezone)
}

/**
 * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class User extends MegaProtoUser[User] {
  def getSingleton = User // what's the "meta" server
  
  object username extends MappedString(this, 100) {
    override def validate = User.find(By(User.username, i_is_!)) match {
      case Full(user) => {
        List(FieldError(this, Text(S.?("username.exists")))) ::: super.validate
      }
      case _ => Nil ::: super.validate
    }
  }
  
  object ipaccesses extends MappedList(this) {
    override def defaultValue = "[]"
  }
  object lastAccess extends MappedDateTime(this)
  object accessRep extends MappedInt(this)
  object reputation extends MappedInt(this)

  def votedForComment(comment: NodeComment): Box[UserCommentVote] =
    UserCommentVote.find(By(UserCommentVote.user, this),
      By(UserCommentVote.comment, comment))
}

