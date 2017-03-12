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

class RankedPairSort(candidates: List[String], ballots: List[List[List[Integer]]]) {
  
  private class Contest {
    import scala.collection.mutable.Map
    
    def keys = candidates
    def entries = ballots
    def count = ballots.length
    def colwidth = keys.length
    def ballots: List[List[List[Integer]]] = List()
    def margins: Map[(Integer, Integer), Integer] = Map()
    
    /*def __init__(self, ents):
        self.entries = ents
        self.count = len(ents)
        self.colwidth = max([ len(key) for key in ents ])
        self.colwidth = max(self.colwidth, 3)
        self.keydict = {}
        for key in ents:
            self.keydict[key] = True
        self.ballots = []
        self.margins = {}*/
    
        
    def isCandidate_?(name: String) = keys.contains(name)
    
    def addBallot(ballot: List[List[Integer]]) = {
      /*
        addballot(list of lists) -> None

        Adds one ballot to the contest. A ballot is a list of ranks;
        each rank is a list of candidate keys.

        Example: if the voter ranks AA first, BB and CC tied for second,
        and DD third ("AA BB/CC DD") then you should call

        contest.addballot([['AA'], ['BB','CC'], ['DD']])

        Ballots need not be complete. All entries in the ballot must be
        valid contest entries, and there must be no duplicates. (This
        method does no consistency checking.)
       */
      ballots :: ballot
    }
    
    def printBallots() = {
      ballots.foldLeft("")((str,item) => str + item.mkString("/"))
    }
    
    def computeMargins() = {
      /*
        Once all the ballots are added, call computemargins() to
        create the margins table.

        This just compares every pair of entries in the ballot list,
        and calls applymargin() if they're not in the same rank (i.e., 
        tied). It hits every pair twice -- once in each direction --
        as required by applymargin().
       */
      ballots.map(ballot => {
        val ranks = ballot.length
        
        (0 to ranks).toList.map(ix => {
          ballot(ix).map(row => {
            (0 to ranks).toList.map(jx => {
              if(jx != ix) {
                ballot(jx).map(col => {
                  applyMargin(row, col, (ix < jx))
                })
              }
            })
          })
        })
      })
      /*
      for ballot in self.ballots:
            ranks = len(ballot)
            for ix in range(ranks):
                for row in ballot[ix]:
                    for jx in range(ranks):
                        if (jx == ix):
                            continue
                        for col in ballot[jx]:
                            self.applymargin(row, col, (ix<jx))
                            */
    }
    
    def applyMargin(row: Integer, col: Integer, rowwins: Boolean) = {
      var value: Integer = margins.getOrElse((row, col), 0)
      
      if(rowwins) value = value + 1
      else value = value - 1
      
      margins += ((row, col) -> value)
    }
    
    def printMargins = {
      // TODO: print margins table to console
    }
    
    def compute = {
      var dic: Map[Integer, List[(Integer, Integer)]] = Map()
      
      margins.keys.map(tup => {
        val value: Integer = margins.getOrElse(tup, 0)
        
        if(value > 0) {
          dic.get(value) match {
            case Some(l) => dic.put(value, { l :+ tup })
            case None => dic.put(value, { tup :: Nil })
          }
        }
      })
      
      val outcome = new Outcome(this)
      
      dic keys match {
        case Nil => { outcome }
        case _ => {
          
          
          
          outcome
        }
      }
    }
    
  }
  
  private class Outcome(contest: Contest) {
    val entries = contest.entries
    var higher: Map[Integer, Map[Integer, Boolean]] = Map()
    var lower: Map[Integer, Map[Integer, Boolean]] = Map()
    
    def printout = {
      
    }
    
    def result = {
      
    }
    
    def printResult = {
      
    }
    
    def cloneOutcome = {
      
    }
    
    def beats(winner: Integer, loser: Integer) = {
      higher.get(loser) match {
        case Some(dic) => {
          dic.get(winner) match {
            case Some(_) => true
            case _ => false
          }
        }
        case _ => false
      }
    }
    
    def compatible(winner: Integer, loser: Integer) = {
      if(winner == loser) throw new Exception("winner cannot beat itself")
      
      (higher get winner, lower get loser) match {
        case (Some(dic), _) => dic.getOrElse(loser, false)
        case (None, Some(dic)) => dic.getOrElse(winner, false)
        case _ => true
      }
    }
    
    def accept(winner: Integer, loser: Integer) = {
      
    }
  }
  
  
}