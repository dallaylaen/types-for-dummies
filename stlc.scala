package stlc;
import scala.collection.immutable.HashMap;
import scala.util.matching.Regex;

class Expr {
    def eval(): Expr = { this }
    def call(e: Expr): Expr = {
        throw new Exception("call() called on a non-function") 
    }
    def apply(arg: Expr) = { new Apply(this, arg) }
    def filter(fun: Expr) = { new Apply(fun, this) }
}

class Apply(fun: Expr, arg: Expr) extends Expr {
    override def toString(): String = {
        fun + "(" + arg + ")";
    }
    override def eval(): Expr = {
        fun.eval().call(arg.eval()).eval()
    }
}

class Native(name: String, body: Expr=>Expr) extends Expr {
    override def call(e: Expr): Expr = {
        return body(e)
    }
    override def toString(): String = {
        return name+"[native]"
    }
}

class Const( name: String ) extends Expr{
    def getName(): String = { name }
    override def toString(): String = { return "\""+name+"\"" }
}

class Sum( name: String, arg: Expr ) extends Const (name){
    override def toString(): String = { return name+"<"+arg+">" }
    override def eval(): Expr = {
        return new Sum(name, arg.eval)
    }
    def getArg(): Expr = { arg }
}

class Product(content: HashMap[String,Expr] = HashMap())  extends Expr{
    def add(name: String, arg: Expr): Product = {
        new Product(content + (name->arg))
    }
    override def toString(): String = {
        "{"+content.keys.toList.sortWith(_<_).map(
            s=>s+":"+content{s} ).mkString(",")+"}"
    }
    override def eval(): Expr = {
        var acc = new HashMap[String,Expr]
        content.keys.foreach( x => { acc = acc + (x->content{x}) } )
        return new Product(acc)
    }

    /* TODO separate Match class? */
    override def call(arg: Expr): Expr = {
        arg match {
            case x:Sum => content{x.getName}.call(x.getArg)
            case x:Const => content{x.getName}.call(new Const("unit"))
            case _ => throw new Exception("Attempt to call match to "+arg)
       }
    }
}

class Parser {
    val re_q = """^\"(.*)\"$""".r
    val re_id = """([a-z][a-z0-9]*)""".r

    var names: HashMap[String,Expr] = new HashMap()

    /* f(x)(y)(z)*/
    def expr(li: Iterator[String], depth: Int=0):Expr = {
        if (!li.hasNext) {
            die ("Expression ended abruptly")
        }
        var first = li.next
        var current: Expr = first match {
            case re_q(content) => new Const(content);
            case re_id(name) => fun(name);
            case any => die( "Unexpected start of expr: "+any );
        }

        var stop: Boolean = false
        while (li.hasNext && !stop) {
            li.next match {
                case "(" => { current = current.apply(expr(li, depth+1)) };
                case ")" => if (depth > 0) {
                    stop = true
                } else {
                    die ("Unexpected closing paren");
                };
                case any => {
                    die ("Unexpected term: '"+any+"'");
                }
            }
        }

        current
    } /* end Parser.expr */

    def beta(name: String, expr: Expr): Unit = {
        names = names + (name->expr)
    }

    def die[T](e: String): T = {
        throw new Exception( "PARSE: "+e )
    }

    def fun(s:String): Expr = {
        if (names.contains(s)) {
            names{s}
        } else {
            die( "Unknown value/function: "+s );
        }
    }
}

object Smoke {
    def main(args: Array[String]): Unit = {
        var expr: Expr = add("const", new Const("42"))
            .add("sum", new Sum("fine", new Const("137")));


        play( expr )

        var I: Expr = fun( "I", x=>x )
        var K: Expr = fun( "K", x=>fun( y=>x ) )
        var S: Expr = fun( "S", f=>fun( g=>fun( arg=>f.apply(arg).apply(g.apply(arg)))))

        var maybe: Expr = new Sum( "maybe", new Const("42") )
        var handle: Expr = new Product().add( "maybe", K ).add( "except", I );

        play(apl(apl(K,new Const("k result")), c("k arg")))

        play(handle.apply(c("maybe","42") ));
        play(handle.apply(c("except","137") ));

        play(S.apply(K).apply(S).apply(c("fff")))

        println(" === PARSER SMOKE === ");

        var par = new Parser()

        par.beta("s", S);
        par.beta("k", K);
        par.beta("i", I);

        play( par.expr("s ( k ) ( i ) ( \"xxx\" )".split(" ").iterator) );

    }

    var n: Int = 1
    def play(e: Expr): Unit = {
        println( "    case "+n )
        println( e )
        println( "    evals to" )
        println( e.eval() )
        println( "    end case "+n )
        n = n+1
    }

    def c(s:String): Expr = { new Const(s) }
    def c(s:String, a:String): Expr = { new Sum(s, c(a)) }
    def fun(name: String, body: Expr=>Expr): Expr = { new Native(name, body) }
    def fun(body: Expr=>Expr): Expr = { new Native(""+body, body) }
    def add(s: String, e:Expr): Product = { new Product().add(s, e) }
    def apl(f: Expr, a:Expr): Expr = { new Apply(f,a) }
}

