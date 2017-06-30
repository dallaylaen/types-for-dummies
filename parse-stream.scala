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
        mid match {
            case x: TodoAscend  => x.context
            case x: TodoDescend => x.merge( descend( src, x ) ).context
            case x: TodoFwd     => descend( src, x )
        }
    }
    def parseLine( rules: Rules, ctx: Ctx, src: ParseTape ): Ctx = {
        descend(src, new TodoFwd(rules, ctx))
    }
}

// ------------------------------------



// ------------------------------------


object Smoke {
    def main (arg: Array[String]): Unit = {
        var tape = new ParseTape("foobared bazooka")

        println ("at "+tape.offset+": "+tape.content)

        var ctx = new Ctx

        var chain = new Rules()
        chain.addRule( """\s*baz\w+""".r, (x, y) => { 
                println( "found "+x+" at "+ tape.where )
                new Todo(new Ctx)
        } );
        chain.addRule( """\s*foo\w+""".r, (x, y) => { 
                println( "found "+x+" at "+ tape.where )
                new Todo(new Ctx)
        } );

        println("pass1");
        chain.grabPrefix(tape, ctx);
        println("pass2");
        chain.grabPrefix(tape, ctx);
        println("pass3");
        chain.grabPrefix(tape, ctx);


    }
}
