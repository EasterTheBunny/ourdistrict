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
import util.BasicTypesHelpers._
import common._
import http._
import rest._


object JsonBillHandler extends RestHelper {

  serve( "api" / "bill" prefix {
    case Nil JsonGet _ =>
      for {
        congressStr <- S.param("congress") ?~ "You must define a congress to narrow the results." ~> 400
        congress <- asInt(congressStr) ?~ "Congress must be parsable to an integer" ~> 400
      } yield {
        try {
          Bill.toJSON(Bill.findAll(By(Bill.congress, congress)))
        } catch {
          case e: Exception =>
        }
        Bill.toJSON(Bill.findAll(By(Bill.congress, congress)))
      }
    case billID :: Nil JsonGet _ =>
      for {
        bill <- Bill.find(By(Bill.bill_id, billID)) ?~ "Bill not found"
      } yield {
        S.param("q") match {
          case Full(q) if q == "text" => {
            /**
              * we need to structure this as a nested tree for easy consumption
              * the client side
              */
            val layers = BillLayer.findAll(By(BillLayer.bill, bill))

            // set the hash lookup
            val lookup = layers.foldLeft(Map[String, BillLayer]())((mp, la) => {
              mp + (la.id.get.toString -> la)
            })

            // collapse the layers
            var roots: List[BillLayer] = Nil
            layers.foreach(la => {
              if(la.parent.isDefined) {
                if(lookup.contains(la.parent.get.toString)) {
                  lookup(la.parent.get.toString) children = lookup(la.parent.get.toString).children :+ la
                }
              } else {
                roots = roots :+ la
              }
            })

            BillLayer.toJSON(roots)
          }
          case _ => Bill.toJSON(bill)
        }
      }
    case billID :: layerID :: Nil JsonGet _ =>
      for {
        bill <- Bill.find(By(Bill.bill_id, billID)) ?~ "Bill not found"
        layer <- BillLayer.find(By(BillLayer.bill, bill), By(BillLayer.hash, layerID)) ?~ "text section not found"
      } yield {
        S.param("q") match {
          case Full(q) if q == "nodes" => {
            val nodes = LayerNode.findAll(By(LayerNode.layer, layer))

            if(nodes.length == 0) {
              LayerNode.toJSON(LayerNode.add(layer.header.get, layer.text.get, layer, Empty) :: Nil)
            } else {
              /**
                * we need to structure this as a nested tree for easy consumption
                * the client side
                */
              // set the hash lookup
              val lookup = nodes.foldLeft(Map[String, LayerNode]())((mp, li) => {
                mp + (li.hash.get -> li)
              })

              // collapse the nodes
              var roots: List[LayerNode] = Nil
              nodes.foreach(li => {
                if(lookup.contains(li.parentHash.get) && li.parent.get != li.id.get) {
                  lookup(li.parentHash.get) children = lookup(li.parentHash.get).children :+ li
                }

                if(li.parent.get == li.id.get) roots = roots :+ li
              })

              LayerNode.toJSON(roots)
            }
          }
          case _ => BillLayer.toJSON(layer)
        }
      }
    case billID :: layerID :: nodeID :: Nil JsonGet _ =>
      for {
        bill <- Bill.find(By(Bill.bill_id, billID)) ?~ "Bill not found"
        layer <- BillLayer.find(By(BillLayer.bill, bill), By(BillLayer.hash, layerID)) ?~ "text section not found"
        node <- LayerNode.find(By(LayerNode.layer, layer), By(LayerNode.hash, nodeID)) ?~ "node not found"
      } yield {
        S.param("q") match {
          case Full(q) if q == "comments" => {
            val comments = NodeComment.findAll(By(NodeComment.node, node))

            if(comments.length == 0) {
              NodeComment.toJSON(comments)
            } else {
              /**
                * we need to structure this as a nested tree for easy consumption
                * the client side
                */
              // set the hash lookup
              val lookup = comments.foldLeft(Map[String, NodeComment]())((mp, nc) => {
                mp + (nc.hash.get -> nc)
              })

              // collapse the nodes
              var roots: List[NodeComment] = Nil
              comments.foreach(nc => {
                if(lookup.contains(nc.parentHash.get) && nc.parent.get != nc.id.get) {
                  lookup(nc.parentHash.get) children = lookup(nc.parentHash.get).children :+ nc
                }

                if(nc.parent.get == nc.id.get) roots = roots :+ nc
              })

              NodeComment.toJSON(roots)
            }
          }
          case _ => LayerNode.toJSON(node)
        }
      }
  })

}
