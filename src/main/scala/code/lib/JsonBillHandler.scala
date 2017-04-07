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
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonDSL._


object JsonBillHandler extends RestHelper {
  /**
    * Convert a JValue to a LiftResponse
    */
  override implicit def jsonToResp(in: JValue): LiftResponse =
    JsonResponse(in,
      ("Content-Type" ->
        "application/vnd.api+json") :: Nil,
      Nil, 200)

  serve( "api" / "v1" / "bills" prefix {
    case Nil JsonGet _ => {
        for {
          congressStr <- S.param("congress") ?~ "You must define a congress to narrow the results." ~> 400
          congress <- asInt(congressStr) ?~ "Congress must be parsable to an integer" ~> 400
        } yield {
          val limit = S.param("limit").map(_.toLong) openOr 1000L
          val offset = S.param("offset").map(_.toLong) openOr 0L

          val retVal: JValue = ("data" -> Bill.toJSON(Bill.findAll(By(Bill.congress, congress), StartAt(offset), MaxRows(limit))))
          retVal
        }
      }
    case Bill(bill) :: Nil JsonGet _ => {
        val retVal: JValue = Bill.toJSON(bill)

        val out: JValue = S.param("include") match {
          case Full(inc) if inc == "parts" => {
            val toMerge: JValue = "relationships" -> ("parts" -> BillLayer.toJSON(BillLayer.layersForBill(bill)))
            val merged = retVal merge toMerge

            "data" -> merged
          }
          case _ => "data" -> retVal
        }

        out
      }
    case Bill(bill) :: "parts" :: Nil JsonGet _ => {
        BillLayer.toJSON(BillLayer.layersForBill(bill))
      }
    case Bill(bill) :: "parts" :: layerID :: Nil JsonGet _ => {
        for {
          layer <- BillLayer.find(By(BillLayer.bill, bill), By(BillLayer.hash, layerID)) ?~ "text section not found"
        } yield {
          val retVal = BillLayer.toJSON(layer)

          val out: JValue = S.param("include") match {
            case Full(inc) if inc == "nodes" => {
              val nodes = LayerNode.nodesForLayer(layer)

              if(nodes.length == 0) {
                val toAdd = LayerNode.add(layer.header.get, layer.text.get, layer, Empty)

                val toMerge: JValue = toAdd match {
                  case Full(nd) =>
                    "relationships" ->
                      ("nodes" -> LayerNode.toJSON(nd :: Nil))
                  case _ =>
                    "relationships" ->
                      ("nodes" -> LayerNode.toJSON(Nil))
                }

                val merged = retVal merge toMerge

                "data" -> merged
              } else {
                val toMerge: JValue = "relationships" ->
                  ("nodes" -> LayerNode.toJSON(nodes))
                val merged = retVal merge toMerge

                "data" -> merged
              }
            }
            case _ => "data" -> retVal
          }

          out
        }
      }
    case Bill(bill) :: "parts" :: layerID :: "nodes" :: Nil JsonGet _ => {
        for {
          layer <- BillLayer.find(By(BillLayer.bill, bill), By(BillLayer.hash, layerID)) ?~ "text section not found"
        } yield {
          val nodes = LayerNode.nodesForLayer(layer)

          if (nodes.length == 0) {
            val toAdd = LayerNode.add(layer.header.get, layer.text.get, layer, Empty)

            toAdd match {
              case Full(nd) => LayerNode.toJSON(nd :: Nil)
              case _ => LayerNode.toJSON(Nil)
            }
          } else {
            LayerNode.toJSON(nodes)
          }
        }
      }
    case Bill(bill) :: "parts" :: layerID :: "nodes" :: nodeID :: Nil JsonGet _ => {
        for {
          layer <- BillLayer.find(By(BillLayer.bill, bill), By(BillLayer.hash, layerID)) ?~ "text section not found"
          node <- LayerNode.find(By(LayerNode.layer, layer), By(LayerNode.hash, nodeID)) ?~ "node not found"
        } yield {
          val retVal = LayerNode.toJSON(node)

          val out: JValue = S.param("include") match {
            case Full(inc) if inc == "comments" => {
              val toMerge: JValue = "relationships" ->
                ("comments" -> NodeComment.toJSON(NodeComment.commentsForNode(node)))
              val merged = retVal merge toMerge

              "data" -> merged
            }
            case _ => "data" -> retVal
          }

          out
        }
      }
  })

  serve( "api" / "v1" / "bills" prefix {
    case Bill(bill) :: "parts" :: layerID :: "nodes" :: nodeID :: Nil JsonPut LayerNode(node) => {
      for {
        layer <- BillLayer.find(By(BillLayer.bill, bill), By(BillLayer.hash, layerID)) ?~ "text section not found"
        node <- LayerNode.find(By(LayerNode.layer, layer), By(LayerNode.hash, nodeID)) ?~ "node not found"
      } yield {
        LayerNode.add(node.statement.get, node.details.get, layer, Full(node)) match {
          case Full(nd) => JsonResponse(LayerNode.toJSON(nd),
                                              ("Content-Type" ->
                                                "application/vnd.api+json") :: Nil,
                                              Nil, 200)
          case _ =>
            val resp: JValue = "errors" -> S.errors.map(_._2.openOr(""))
            JsonResponse(resp, ("Content-Type" -> "application/vnd.api+json") :: Nil, Nil, 400)
        }
      }
    }
    case Bill(bill) :: "parts" :: layerID :: "nodes" :: nodeID :: Nil JsonPost json => {
      for {
        layer <- BillLayer.find(By(BillLayer.bill, bill), By(BillLayer.hash, layerID)) ?~ "text section not found"
        node <- LayerNode.find(By(LayerNode.layer, layer), By(LayerNode.hash, nodeID)) ?~ "node not found"
      } yield {
        val retVal = LayerNode.toJSON(node)

        val out: JValue = S.param("include") match {
          case Full(inc) if inc == "comments" => {
            val toMerge: JValue = "relationships" ->
              ("comments" -> NodeComment.toJSON(NodeComment.commentsForNode(node)))
            val merged = retVal merge toMerge

            "data" -> merged
          }
          case _ => "data" -> retVal
        }

        out
      }
    }
  })

}
