package stlc.parser

import scala.util.matching.Regex
import scala.collection.immutable.HashMap
import stlc.la._

class Error (s: String) extends Exception("PARSE: "+s) {
}

class Lex {
    var content : List[List[String]] = List()

    var line = 0

    val re_comm = """^\s*#""".r
    val re_newl = "\n".r

    val re_name = """\w+""".r
    val re_key = """[~(){}\[\]\.:=<>]""".r
    val re_space = """\s+""".r

    val re_chunk = new Regex("("+List(re_key, re_name, re_space, ".+").mkString(")|(")+")")

    def slurp(all: String): Lex = {
        var cut = re_newl.split(all).toList

        println ("got lines")
        cut.foreach( str => {
            line = line + 1
            println ("line "+line)

            var it = re_chunk.findAllIn(str)
            var terms: List[String] = List()
            while (it.hasNext) {
                println("line "+line+": found term "+it.group(0) )
                if (it.group(1) != null) terms = terms :+ it.next
                else if (it.group(2) != null) terms = terms :+ it.next
                else if (it.group(3) != null) {it.next}
                else {
                    throw new Error("Garbage in line "+line
                        +" starting at --->"+it.next)
                }
            }
            if (terms.length > 0) content = content :+ terms
        })
        this
    }

    def getLines(): List[List[String]] = { content }
}

class Parser {
    var ctx = new Context(new HashMap())
    var names: HashMap[String,FreeVar] = new HashMap()
    var types: HashMap[String,Type] = new HashMap()

    def checkName(n: String): String = {
        if (names.contains(n))
            throw new Error ("Name already exists: "+n+": "+ctx.getValue(names{n}))
        if (types.contains(n))
            throw new Error ("Name already exists: "+n+": "+types{n})
        return n
    }

    def parseLine(terms: Iterator[String]):Unit = terms.next match {
        case 'type' => {
            var name = terms.next
            if (terms.hasNext)
                throw new Error ("Unexpected terms after type def: "+name)
            addType(checkName(name))
        }
        case 'cons' => {
            var ty = terms.next
        }
        case any: String => throw new Error ("Unknown directive "+any)
    }
}




object Smoke {
    def main(arg: Array[String]): Unit = {
        var lex = new Lex()
        lex.slurp("""
This is a story
of := and ==
  
that ends well
        """)

        lex.getLines.foreach(
            li => {
                println( "Found terms: "+li.mkString(",") )
            }
        )
    }
}
