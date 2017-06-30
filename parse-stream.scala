package stlc.parse;

import scala.util.matching.Regex;
import scala.util.matching.Regex.Match;

class ParseError(s: String) extends Exception("PARSE: "+s) {
    def reason(): String = {s}
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
    var isFinal = false

    def makeFinal(): Rules = {
        isFinal = true
        this
    }

    def addRule( r: Regex, todo: Handler ): Rules = {
        rules = rules :+ Pair(r, todo)
        this
    }

    def grabPrefix( src: ParseTape, ctx: Ctx ): Todo = {
        for (pair <- rules) {
            src.grabPrefix(pair._1) match {
                case None => {}
                case Some(hit: Match) => {
                    println("\t[match] "+hit+" matches "+pair._1+" at "+src.where)
                    return pair._2(hit, ctx)
                }
            }
        }
        throw new ParseError("No rule found for '"+src+"'"+" at "+src.where);
    }
}

class Todo(ctx: Ctx) {
    def context(): Ctx = { ctx }
    def stopHere(): Ctx = { ctx }
}

class TodoFwd(rules: Rules, ctx: Ctx) extends Todo(ctx) {
    def grabPrefix(src: ParseTape): Todo = {
        rules.grabPrefix(src, ctx)
    }
    override def stopHere(): Ctx = {
        if (rules.isFinal)
            return ctx
        throw new ParseError("Parse stopped in non-final state")
    }
}

class TodoDescend(rules: Rules, ctx: Ctx, imerge: Ctx=>Todo)
        extends TodoFwd(rules, ctx) {
    def merge(ctx: Ctx): Todo = { imerge(ctx) }
    def getMerge(): Ctx=>Todo = { imerge }
    override def stopHere(): Ctx = {
        throw new ParseError("Cannot stop at descend")
    }
}

class TodoAscend(ctx: Ctx) extends Todo(ctx) {
    override def stopHere(): Ctx = { throw new ParseError("Unfinished parse") }
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
        var todo:Todo = new TodoFwd(rules, ctx)
        var stack: List[Ctx=>Todo] = List()

        try {
            while (true) {
                todo match {
                    case x: TodoDescend => {
                        println("\t[stack] push at "+src.where)
                        stack = x.getMerge :: stack
                    }
                    case _ =>
                }
                todo match {
                    case x: TodoAscend  => {
                        if (stack.length == 0)
                            throw new ParseError("Ascend unexpected")
                        println("\t[stack] pop at "+src.where)
                        todo  = stack.head(x.context);
                        stack = stack.tail
                    }
                    case x: TodoFwd     => {
                        todo  = x.grabPrefix(src)
                    }
                    case _ => throw new ParseError("Don't know how to handle "+todo)
                }
                if (!src.nonempty && stack.length == 0) {
                    return todo.stopHere
                }
            } //
        } catch {
            case e: ParseError => throw new ParseError(e.reason+" at "+src.where)
            case other => throw other
        }
        throw new ParseError("Cannot reach here")
    } // end of parseLine
}

// ------------------------------------

class MyCtx( s: String ) extends Ctx {
    override def toString(): String = s
}

// ------------------------------------


object Smoke {
    def main (arg: Array[String]): Unit = {
        var tape = new ParseTape(arg(0))

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
        expr.makeFinal

        var cycle = new ParserCycle()

        try {
            println( "Got it: "+cycle.parseLine( expr, new MyCtx("parens"), tape ) )
        } catch {
            case e: ParseError => println(e)
            case other => throw other
        }


    }
}
