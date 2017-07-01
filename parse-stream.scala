package stlc.parse;

import scala.collection.immutable.HashMap;
import scala.util.matching.Regex;
import scala.util.matching.Regex.Match;

class ParseError(s: String) extends Exception("PARSE: "+s) {
    def reason(): String = {s}
}

class ParseTape (init: String, iline: Int = 1, ioffset: Int = 1) {
    var content = init
    var line    = iline
    var char    = ioffset
    var lineNow = iline
    var charNow = ioffset

    def grabPrefix( rex: Regex ): Option[Match] = {
        rex.findPrefixMatchOf(content) match {
            case None => return None
            case Some(hit: Match) => {
                content = content.substring(hit.matched.length, content.length)
                /* ouch! */
                char    = charNow
                line    = lineNow
                var newlines = "\n[^\n]*".r.findAllIn(hit.matched).toList
                if (newlines.length > 0) {
                    lineNow = lineNow + newlines.length
                    charNow = newlines.last.length
                } else {
                    charNow = charNow + hit.matched.length
                }
                return Option(hit)
            }
        }
    }
    def nonempty(): Boolean = { content.length > 0 }

    def where(): String = { "line "+line+" offset "+char }
    def whereNow(): String = { "line "+lineNow+" offset "+charNow }
}

class Rules[Ctx] (name: String) {
    type Handler = (Match,Ctx)=>Todo[Ctx]
    var rules : List[Pair[Regex,Handler]] = List()
    var isFinal = false
    var locked = false
    var xpadding = "".r

    def lock(): Rules[Ctx] = {
        if (locked)
            throw new Exception("Attempt to modify locked state")
        if (rules.length == 0)
            throw new Exception("Attempt to lock an empty state "+name)
        locked = true
        return this
    }

    def makeFinal(): Rules[Ctx] = {
        if (locked)
            throw new Exception("Attempt to modify locked state")
        isFinal = true
        this
    }

    def addRule( r: Regex, todo: Handler ): Rules[Ctx] = {
        if (locked)
            throw new Exception("Attempt to modify locked state")
        rules = rules :+ Pair(r, todo)
        this
    }

    def padding(rex: Regex): Rules[Ctx] = {
        if (locked)
            throw new Exception("Attempt to modify locked state")
        xpadding = rex
        this
    }

    def grabPrefix( src: ParseTape, ctx: Ctx ): Todo[Ctx] = {
        if (!locked)
            throw new Exception("Attempt to parse using unlocked rule")
        src.grabPrefix(xpadding) match {
            case Some(whitespace: Match) =>
            case None => throw new ParseError("Padding failed to match for state "+name+": "+xpadding)
        }
        for (pair <- rules) {
            src.grabPrefix(pair._1) match {
                case None => {}
                case Some(hit: Match) => {
                    println("\t[match] "+hit+" matches "+pair._1+" in "+name+" at "+src.where)
                    return pair._2(hit, ctx)
                }
            }
        }
        throw new ParseError("No matching rule found in "+name);
    }
}

class Todo[Ctx](ctx: Ctx) {
    def context(): Ctx = { ctx }
    def stopHere(): Ctx = { ctx }
}

class TodoFwd[Ctx](rules: Rules[Ctx], ctx: Ctx) extends Todo[Ctx](ctx) {
    def grabPrefix(src: ParseTape): Todo[Ctx] = {
        rules.grabPrefix(src, ctx)
    }
    override def stopHere(): Ctx = {
        if (rules.isFinal)
            return ctx
        throw new ParseError("Parse stopped in non-final state")
    }
}

class TodoDescend[Ctx](rules: Rules[Ctx], ctx: Ctx, imerge: Ctx=>Todo[Ctx])
        extends TodoFwd[Ctx](rules, ctx) {
    def merge(ctx: Ctx): Todo[Ctx] = { imerge(ctx) }
    def getMerge(): Ctx=>Todo[Ctx] = { imerge }
    override def stopHere(): Ctx = {
        throw new ParseError("Cannot stop at descend")
    }
}

class TodoAscend[Ctx](ctx: Ctx) extends Todo[Ctx](ctx) {
    override def stopHere(): Ctx = { throw new ParseError("Unfinished parse") }
}





class ParserCycle[Ctx] {
    var locked = false
    var states: HashMap[String,Rules[Ctx]] = new HashMap()
    var start: String = ""
    var xpadding: Option[Regex] = None

    def lock(): ParserCycle[Ctx] = {
        if (start == "")
            throw new Exception("Attempt to lock parser without starting state")
        states.values.toList.foreach( x => x.lock )
        locked = true
        this
    }

    def state(s: String): Rules[Ctx] = { states{s} }
    def addState(s: String): Rules[Ctx] = {
        if (locked)
            throw new Exception("Attempt to modified locked parser")
        if (states.contains(s))
            throw new Exception("Duplicate definition of state "+s)
        var r = new Rules[Ctx](s)
        xpadding match {
            case Some(rex: Regex) => r.padding(rex)
            case _ =>
        }
        states = states + (s -> r)
        r
    }
    def startState(s: String): Rules[Ctx] = {
        if (locked)
            throw new Exception("Attempt to modified locked parser")
        if (start != "")
            throw new Exception("Attempt to replace starting state "+start+" with "+s)
        start = s
        addState(s)
    }
    def padding(): ParserCycle[Ctx] = {
        xpadding = None
        this
    }
    def padding(rex: Regex): ParserCycle[Ctx] = {
        xpadding = Option(rex)
        this
    }

    def checkSwitch(from: String, to: String): Unit = {
        if (locked)
            throw new Exception("Attempt to modified locked parser")
        if (!states.contains(from))
            throw new Exception("Attempt to add transition from nonexistent state "+from);
        if (to != "" && !states.contains(to))
            throw new Exception("Attempt to add transition to nonexistent state "+to);
    }

    def switchAscend(from: String, rex: Regex, body:(Match,Ctx)=>Ctx): ParserCycle[Ctx] = {
        checkSwitch(from, "")
        var fromState = states{from}
        fromState.addRule(rex, (term, ctx) => new TodoAscend[Ctx](body(term,ctx)))

        this
    }
    def switchFwd(from: String, rex: Regex, to: String, body:(Match,Ctx)=>Ctx): ParserCycle[Ctx] = {
        checkSwitch(from, to)
        var fromState = states{from}
        var toState   = states{to}
        fromState.addRule(rex, (term, ctx) => new TodoFwd[Ctx](toState, body(term,ctx)))

        this
    }
    def switchDescend(from: String, rex: Regex, to: String, body:(Match,Ctx)=>Ctx, merge: (Ctx,Ctx)=>Todo[Ctx]): ParserCycle[Ctx] = {
        checkSwitch(from, to)
        var fromState = states{from}
        var toState   = states{to}
        fromState.addRule(rex, (term, ctx) => new TodoDescend[Ctx](toState, body(term,ctx), inner => merge(ctx, inner)))

        this
    }

    def parseLine( ctx: Ctx, src: ParseTape ): Ctx = {
        if (!locked)
            throw new Exception("Trying to use a non-locked machine");

        var todo:Todo[Ctx] = new TodoFwd[Ctx](states{start}, ctx)
        var stack: List[Ctx=>Todo[Ctx]] = List()

        /* We'll use unchecked here, BUT we know for sure
         * that Ctx is what it claims to be */
        try {
            while (true) {
                todo match {
                    case x: TodoDescend[Ctx] => {
                        println("\t[stack] push at "+src.where)
                        stack = x.getMerge :: stack
                    }
                    case _ =>
                }
                todo match {
                    case x: TodoAscend[Ctx] => {
                        if (stack.length == 0)
                            throw new ParseError("Ascend unexpected")
                        println("\t[stack] pop at "+src.where)
                        todo  = stack.head(x.context);
                        stack = stack.tail
                    }
                    case x: TodoFwd[Ctx] => {
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

        var cycle = new ParserCycle[MyCtx]().padding("\\s*".r)
        cycle.startState("expr")
        var st_arg = cycle.addState("arg").makeFinal

        cycle.switchFwd("expr", "\\w+".r, "arg", (term, ctx) => new MyCtx(""+term))
        cycle.switchDescend("arg", "\\(".r, "expr", (_, ctx) => new MyCtx("."), (outer, inner) => new TodoFwd[MyCtx](st_arg, new MyCtx(outer+"["+inner+"]")))
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
