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
package snippet

import net.liftweb._
import common._
import util._
import Helpers._
import http._
import sitemap._

import scala.xml.Text

import code.model._

object TopicPage {
  def menu = Menu.param[Topic]("Topic", Loc.LinkText(topic => Text(topic.hash.get)), Topic.searchByHash _, _.hash.get) / "topic" / *
}

class TopicPage(topic: Topic) {
    
  def render = 
  "#frame [topic]" #> topic.hash.get &
  "@topic-details" #> topic.details.get /*&
  "@title *" #> topic.details.is &
  "@stock *" #> (if(item.stock.is > 1) "in stock" else "out of stock") &
  "@chart *" #> prices(item.prices.prices) &
  "@color *" #> ("color: " + item.color.is) &
  "@add_to_cart [onclick]" #> SHtml.ajaxInvoke(() => TheShoppingCart.addItem(item)) &
  "@description" #> item.description.withLineBreaks*/
  
  /*
  def prices(in: Seq[PriceCase]) = {
    "tr" #> in.map(price => {
      "@qnty *" #> price.threshold &
      "@prc *" #> PreferredCurrency.make(0).from(US.make(price.price)).format
    })
  }*/
}

