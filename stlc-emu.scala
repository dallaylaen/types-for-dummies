package stlc.emu

import scala.collection.immutable.HashMap;
import scala.util.matching.Regex;
import scala.util.matching.Regex.Match;
import stlc.la._
import stlc.parse._

class Ctx {
    var types: HashMap[String,Type] = new HashMap()

    def checkName(name: String): Unit = {
        if (types.contains(name))
            throw new Exception("Name already registeredi (type): "+name)
    }

    def addType(name: String): Ctx = {
        checkName(name)
        types = types + (name -> new PartialType(name))
        this
    }

    override def toString(): String = {
        types.keys.toList.sortWith(_<_).map(s=>"type "+s).mkString("\n")
    }
}

object Parse {
    var machine = new ParserCycle[Ctx]()
   
    val re_nl = "\n+".r
    val re_id = "\\b[A-Za-z][A-Za-z_0-9]*\\b".r

    val same: (Any, Ctx) => Ctx = (_, x)=>x

    val term: String => Regex = s => new Regex("\\b"+s+"\\b")

    machine.padding(" *".r)

    machine.startState("start").makeFinal
    machine.addState("eol")
    machine.addState("type")

    machine.switch("start", re_nl, "start", same)

    machine.switch("eol", "\n".r, "start", same)   

    machine.switch("start", term("type"), "type", same)
    machine.switch("type", re_id, "eol", (id, ctx) => ctx.addType(id.matched))

    machine.switch("start", "$".r, "start", same)
    machine.lock

    def run(src: String): Ctx = {
        return machine.parseLine(new Ctx(), new ParseTape(src))
    }
}

object Smoke {
    def main (args: Array[String]) {
        var sample = """
type T
        """

        try {
            println( Parse.run(sample) )
        } catch {
            case e: ParseError => println( e );
        }

    };
}






