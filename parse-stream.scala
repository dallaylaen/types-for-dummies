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
    def nonempty(): Boolean = { content.length > 0 }

    def whereNow(): String = { "line "+line+" offset "+offset }
    def where(): String = { "line "+line+" offset "+prev }
}


class Rules[Ctx] {
    type Handler = (Match,Ctx)=>Todo[Ctx]
    var rules : List[Pair[Regex,Handler]] = List()
    def addRule( r: Regex, todo: Handler ): Rules[Ctx] = {
        rules = rules :+ Pair(r, todo)
        this
    }

    def grabPrefix( src: ParseTape, ctx: Ctx ): Todo[Ctx] = {
        for (pair <- rules) {
            src.grabPrefix(pair._1) match {
                case None => {}
                case Some(hit: Match) => return pair._2(hit, ctx)
            }
        }
        throw new ParseError("No rule found for '"+src+"'"+" at "+src.where);
    }
}

class Todo[Ctx] {
}

class TodoFwd[Ctx](rules: Rules[Ctx], ctx: Ctx) extends Todo[Ctx] {
    def grabPrefix(src: ParseTape): Todo[Ctx] = {
        rules.grabPrefix(src, ctx)
    }
}

class TodoDescend[Ctx](rules: Rules[Ctx], ctx: Ctx, imerge: Ctx=>Todo[Ctx])
        extends TodoFwd[Ctx](rules, ctx) {
    def merge(ctx: Ctx): Todo[Ctx] = { imerge(ctx) }
}

class TodoAscend[Ctx](ctx: Ctx) {
    def context(): Ctx = { ctx }
}





class ParserCycle[Ctx] {
    def descend(src: ParseTape, start: Todo[Ctx]): Ctx = {
        throw new Exception ("not done");
    }
    def parseLine( rules: Rules[Ctx], ctx: Ctx, src: ParseTape ): Ctx = {
        descend(src, new TodoFwd(rules, ctx))
    }
}

// ------------------------------------



// ------------------------------------


object Smoke {
    def main (arg: Array[String]): Unit = {
        var tape = new ParseTape("foobared bazooka")

        println ("at "+tape.offset+": "+tape.content)

        var ctx = ""

        var chain = new Rules[String]()
        chain.addRule( """\s*baz\w+""".r, (x, y) => { 
                println( "found "+x+" at "+ tape.where )
                new Todo[String]()
        } );
        chain.addRule( """\s*foo\w+""".r, (x, y) => { 
                println( "found "+x+" at "+ tape.where )
                new Todo[String]()
        } );

        println("pass1");
        chain.grabPrefix(tape, ctx);
        println("pass2");
        chain.grabPrefix(tape, ctx);
        println("pass3");
        chain.grabPrefix(tape, ctx);


    }
}
