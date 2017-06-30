package stlc.parse;

import scala.collection.immutable.HashMap;
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


class Rules (name: String) {
    type Handler = (Match,Ctx)=>Todo
    var rules : List[Pair[Regex,Handler]] = List()
    var isFinal = false
    var locked = false

    def lock(): Rules = {
        if (locked)
            throw new Exception("Attempt to modify locked state")
        if (rules.length == 0)
            throw new Exception("Attempt to lock an empty state "+name)
        locked = true
        return this
    }

    def makeFinal(): Rules = {
        if (locked)
            throw new Exception("Attempt to modify locked state")
        isFinal = true
        this
    }

    def addRule( r: Regex, todo: Handler ): Rules = {
        if (locked)
            throw new Exception("Attempt to modify locked state")
        rules = rules :+ Pair(r, todo)
        this
    }

    def padding(rex: Regex): Rules = {
        new Rules("padding of "+name).addRule( rex,
            (_, ctx) => new TodoFwd(this, ctx) ).lock
    }

    def grabPrefix( src: ParseTape, ctx: Ctx ): Todo = {
        if (!locked)
            throw new Exception("Attempt to parse using unlocked rule")
        for (pair <- rules) {
            src.grabPrefix(pair._1) match {
                case None => {}
                case Some(hit: Match) => {
                    println("\t[match] "+hit+" matches "+pair._1+" in "+name+" at "+src.where)
                    return pair._2(hit, ctx)
                }
            }
        }
        throw new ParseError("No matching rule found in "+name+" at "+src.where);
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
    type Handler = (Match,Ctx)=>Todo

    var states: HashMap[String,Rules] = new HashMap()
    var start: String = ""
    var locked = false

    def lock(): ParserCycle = {
        if (start == "")
            throw new Exception("Attempt to lock parser without starting state")
        states.values.toList.foreach( x => x.lock )
        locked = true
        this
    }

    def state(s: String): Rules = { states{s} }
    def addState(s: String): Rules = {
        if (locked)
            throw new Exception("Attempt to modified locked parser")
        if (states.contains(s))
            throw new Exception("Duplicate definition of state "+s)
        var r = new Rules(s)
        states = states + (s -> r)
        r
    }
    def startState(s: String): Rules = {
        if (locked)
            throw new Exception("Attempt to modified locked parser")
        if (start != "")
            throw new Exception("Attempt to replace starting state "+start+" with "+s)
        start = s
        addState(s)
    }

    def checkSwitch(from: String, to: String): Unit = {
        if (locked)
            throw new Exception("Attempt to modified locked parser")
        if (!states.contains(from))
            throw new Exception("Attempt to add transition from nonexistent state "+from);
        if (to != "" && !states.contains(to))
            throw new Exception("Attempt to add transition to nonexistent state "+to);
    }

    def switchAscend(from: String, rex: Regex, body:(Match,Ctx)=>Ctx): ParserCycle = {
        checkSwitch(from, "")
        var fromState = states{from}
        fromState.addRule(rex, (term, ctx) => new TodoAscend(body(term,ctx)))

        this
    }
    def switchFwd(from: String, rex: Regex, to: String, body:(Match,Ctx)=>Ctx): ParserCycle = {
        checkSwitch(from, to)
        var fromState = states{from}
        var toState   = states{to}
        fromState.addRule(rex, (term, ctx) => new TodoFwd(toState, body(term,ctx)))

        this
    }
    def switchDescend(from: String, rex: Regex, to: String, body:(Match,Ctx)=>Ctx, merge: (Ctx,Ctx)=>Todo): ParserCycle = {
        checkSwitch(from, to)
        var fromState = states{from}
        var toState   = states{to}
        fromState.addRule(rex, (term, ctx) => new TodoDescend(toState, body(term,ctx), inner => merge(ctx, inner)))

        this
    }

    def parseLine( ctx: Ctx, src: ParseTape ): Ctx = {
        if (!locked)
            throw new Exception("Trying to use a non-locked machine");

        var todo:Todo = new TodoFwd(states{start}, ctx)
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

        var cycle = new ParserCycle()
        cycle.startState("expr")
        var st_arg = cycle.addState("arg").makeFinal

        cycle.switchFwd("expr", "\\w+".r, "arg", (term, ctx) => new MyCtx(""+term))
        cycle.switchDescend("arg", "\\(".r, "expr", (_, ctx) => new MyCtx("."), (outer, inner) => new TodoFwd(st_arg, new MyCtx(outer+"["+inner+"]")))
        cycle.switchAscend("arg", "\\)".r, (_, ctx) => ctx)
        cycle.switchAscend("expr", "\\)".r, (_, ctx) => new MyCtx("nil"))
        cycle.lock

        try {
            println( "Got it: "+cycle.parseLine( new MyCtx(""), tape ) )
        } catch {
            case e: ParseError => println(e)
        }


    }
}
