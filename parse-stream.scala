package stlc.parse;

import scala.util.matching.Regex;
import scala.util.matching.Regex.Match;

class ParseError(s: String) extends Exception("PASRE: "+s) {
}



class ParseTape (init: String, iline: Int = 0, ioffset: Int = 1) {
    var content = init
    var line    = iline
    var offset  = ioffset
    var prev    = ioffset

    def grabPrefix( rex: Regex ): Option[Match] = {
        rex.findPrefixMatchOf(content) match {
            case None => return None
            case Some(hit: Match) => {
                content = content.substring(hit.matched.length, content.length)
                /* ouch! */
                prev    = offset
                offset  = offset + hit.matched.length
                return Option(hit)
            }
        }
    }
    def where(): String = { "line "+line+" offset "+offset }
    def whereLast(): String = { "line "+line+" offset "+prev }
}

class ParseContext {
    def merge( other: ParseContext ) {
        throw new ParseError("unimplemented");
    }
}


class ParseOutcome {
    
}

class ParseExpect {
    type Handler = (Match,ParseContext)=>ParseOutcome
    var rules : List[Pair[Regex,Handler]] = List()
    def addRule( r: Regex, todo: Handler ): ParseExpect = {
        rules = rules :+ Pair(r, todo)
        this
    }

    def grabPrefix( src: ParseTape, ctx: ParseContext ): ParseOutcome = {
        for (pair <- rules) {
            src.grabPrefix(pair._1) match {
                case None => {}
                case Some(hit: Match) => return pair._2(hit, ctx)
            }
        }
        throw new ParseError("No rule found for '"+src+"'"+" at "+src.where);
    }
    
}

object Smoke {
    def main (arg: Array[String]): Unit = {
        var tape = new ParseTape("foobared bazooka")

        println ("at "+tape.offset+": "+tape.content)

        var ctx = new ParseContext

        var chain = new ParseExpect()
        chain.addRule( """\s*baz\w+""".r, (x, y) => { 
                println( "found "+x+" at "+ tape.whereLast )
                new ParseOutcome()
        } );
        chain.addRule( """\s*foo\w+""".r, (x, y) => { 
                println( "found "+x+" at "+ tape.whereLast )
                new ParseOutcome()
        } );

        println("pass1");
        chain.grabPrefix(tape, ctx);
        println("pass2");
        chain.grabPrefix(tape, ctx);
        println("pass3");
        chain.grabPrefix(tape, ctx);


    }
}
