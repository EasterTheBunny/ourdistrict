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
import scala.xml.{Text, XML}
import io.github.cloudify.scala.spdf._
import java.io._

object PDFServer extends RestHelper {
  
  serve {
    case "pdf" :: "topic" :: Topic(topic) :: Nil Get req if req.path.suffix == "pdf" =>
      
      /*
       * find longest sequence of nodes
       * 
       * 
       */
      val nodelist = S.param("node-list") ?~ "text parameter missing" ~> 404
      //println(nodelist)
      //val nodes = topic.nodes;
      
      var nodes: List[Node] = List()
      
      nodelist match {
        case Full(list) => {
          val searchTerms = list.split(";");
          val hashAndVersion = searchTerms.foldLeft(List[(String, String)]())((lst:List[(String,String)], str:String) => (str.split(":") head, str.split(":").reverse.head) :: lst)
          val hashAndVersionTuple = hashAndVersion unzip
          
          nodes = Node.findAll(By(Node.topic, topic.id.get), ByList(Node.hash, hashAndVersionTuple._1), ByList(Node.version, hashAndVersionTuple._2.map(a => a.toInt))) filter (node => {
            hashAndVersion.indexOf((node.hash.get, node.version.get.toString)) >= 0
          })
        }
        case _ => nodes = List()
      }
      
      val headers = 
        	//("Last-Modified" -> toInternetDate(key.startTime.is.getTime)) ::
        	("Last-Modified" -> toInternetDate(new java.util.Date())) ::
        	("Content-type" -> "application/pdf") :: Nil
      
      try{
        val pdf = Pdf("/usr/local/bin/wkhtmltopdf", new PdfConfig {
    	  orientation := Landscape
    	  pageSize := "Letter"
    	  marginTop := "1in"
    	  marginBottom := "1in"
    	  marginLeft := "1in"
    	  marginRight := "1in"
        })
      
        val page = (<html>
        		<head>
        			<link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css" />
        		</head>
        		<body>
        		<h1>{ topic.title.get }</h1>
        		<h2>{ topic.details.get }</h2>
        		<div>
        			{ nodes.map(node => (
        			  <hr/>
        			  <div><h3>{ node.statement.get }</h3></div>
        			  <div>{ XML.loadString("<div>" + node.details.get + "</div>") }</div>
        			)) }
        		</div>
        		</body></html>)
        val outputStream = new ByteArrayOutputStream
        pdf.run(page, outputStream)
      
        val data = outputStream.toByteArray()
        outputStream.close()
      
        Full(InMemoryResponse(data, headers, Nil, 200))
      } catch {
        case e: Exception => Full(InMemoryResponse(new Array[Byte](0), headers, Nil, 404))
      }
    
    case "pdf" :: "node" :: Node(node) :: Nil Get req if req.path.suffix == "pdf" =>
      
      val headers = 
        	//("Last-Modified" -> toInternetDate(key.startTime.is.getTime)) ::
        	("Last-Modified" -> toInternetDate(new java.util.Date())) ::
        	("Content-type" -> "application/pdf") :: Nil
        	
      try{
        val pdf = Pdf("/usr/local/bin/wkhtmltopdf", new PdfConfig {
    	  orientation := Landscape
    	  pageSize := "Letter"
    	  marginTop := "1in"
    	  marginBottom := "1in"
    	  marginLeft := "1in"
    	  marginRight := "1in"
        })
      
        val page = (<html>
        		<head>
        			<link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css" />
        		</head>
        		<body>
        		<p class="lead">{ node.statement.get }</p>
        		<div>{ XML.loadString("<div>" + node.details.get + "</div>") }</div>
        		</body></html>)
        val outputStream = new ByteArrayOutputStream
        pdf.run(page, outputStream)
      
        val data = outputStream.toByteArray()
        outputStream.close()
      
        Full(InMemoryResponse(data, headers, Nil, 200))
      } catch {
        case e: Exception => {
          println(e);
          Full(InMemoryResponse(new Array[Byte](0), headers, Nil, 404))
        }
      }
      
    case "pdf" :: "comments" :: Node(node) :: Nil Get req if req.path.suffix == "pdf" =>
      
      val comments = node.comments
      
      val headers = 
        	//("Last-Modified" -> toInternetDate(key.startTime.is.getTime)) ::
        	("Last-Modified" -> toInternetDate(new java.util.Date())) ::
        	("Content-type" -> "application/pdf") :: Nil
        	
      try{
        val pdf = Pdf("/usr/local/bin/wkhtmltopdf", new PdfConfig {
    	  orientation := Landscape
    	  pageSize := "Letter"
    	  marginTop := "1in"
    	  marginBottom := "1in"
    	  marginLeft := "1in"
    	  marginRight := "1in"
        })
      
        val page = (<html>
        		<head>
        			<link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css" />
        		</head>
        		<body>
        		<div>{ comments.map(comment => (
        				<h5>username</h5>
        				<div>{ XML.loadString("<div>" + comment.text.get + "</div>") }</div>
        		)) }</div>
        		</body></html>)
        val outputStream = new ByteArrayOutputStream
        pdf.run(page, outputStream)
      
        val data = outputStream.toByteArray()
        outputStream.close()
      
        Full(InMemoryResponse(data, headers, Nil, 200))
      } catch {
        case e: Exception => {
          println(e);
          Full(InMemoryResponse(new Array[Byte](0), headers, Nil, 404))
        }
      }
  }
  
}