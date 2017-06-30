package stlc.parse;

import scala.util.matching.Regex;
import scala.util.matching.Regex.Match;

class ParseError(s: String) extends Exception("PARSE: "+s) {
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

class Ctx {
}


class Rules {
    type Handler = (Match,Ctx)=>Todo
    var rules : List[Pair[Regex,Handler]] = List()
    def addRule( r: Regex, todo: Handler ): Rules = {
        rules = rules :+ Pair(r, todo)
        this
    }

    def grabPrefix( src: ParseTape, ctx: Ctx ): Todo = {
        for (pair <- rules) {
            src.grabPrefix(pair._1) match {
                case None => {}
                case Some(hit: Match) => return pair._2(hit, ctx)
            }
        }
        throw new ParseError("No rule found for '"+src+"'"+" at "+src.where);
    }
}

class Todo(ctx: Ctx) {
    def context(): Ctx = { ctx }
}

class TodoFwd(rules: Rules, ctx: Ctx) extends Todo(ctx) {
    def grabPrefix(src: ParseTape): Todo = {
        rules.grabPrefix(src, ctx)
    }
}

class TodoDescend(rules: Rules, ctx: Ctx, imerge: Ctx=>Todo)
        extends TodoFwd(rules, ctx) {
    def merge(ctx: Ctx): Todo = { imerge(ctx) }
}

class TodoAscend(ctx: Ctx) extends Todo(ctx) {
}





class ParserCycle {
    def descend(src: ParseTape, start: Todo): Ctx = {
        var mid: Todo = start match {
            case x: TodoFwd     => x.grabPrefix(src)
        }
        while (true) {
            mid match {
                case x: TodoDescend => mid = x.merge( descend( src, x ) )
                case x: TodoAscend  => return x.context
                case x: TodoFwd     => return descend( src, x )
            }
        }
        throw new ParseError("Control never gets here, file a bug");
    }
    def parseLine( rules: Rules, ctx: Ctx, src: ParseTape ): Ctx = {
        descend(src, new TodoFwd(rules, ctx))
    }
}

// ------------------------------------

class MyCtx( s: String ) extends Ctx {
    override def toString(): String = s
}

// ------------------------------------


object Smoke {
    def main (arg: Array[String]): Unit = {
        var tape = new ParseTape("(()(()()))")

        println (tape.content + " at " + tape.where)

        var expr    = new Rules()
        expr.addRule( "\\(".r, (term, ctx) => {
            println( "descend: "+term+" at "+tape.where )
            new TodoDescend(expr, new MyCtx(""), c => new TodoFwd(
                expr, new MyCtx(ctx+"["+c+"]" ) ) )
        } );
        expr.addRule( "\\)".r, (term, ctx) => {
            println( "ascend:  "+term+" at "+tape.where )
            new TodoAscend( ctx )
        } );
        expr.addRule( "$".r, (term, ctx) => {
            println( "eol at "+tape.where );
            new TodoAscend( ctx )
        } );

        var cycle = new ParserCycle()
        println( cycle.parseLine( expr, new MyCtx("parens"), tape ) )



    }
}
