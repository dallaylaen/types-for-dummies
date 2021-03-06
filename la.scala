package stlc.la;

import scala.collection.immutable.HashMap;

class Error(s: String) extends Exception(s) {
    override def toString(): String = {
        return super.toString + " at line " + getStackTrace()(1).getLineNumber + " at line " + getStackTrace()(2).getLineNumber + " at line " + getStackTrace()(3).getLineNumber
    }
}

object u { /* Universe */
    var known: HashMap[String,Type] = new HashMap()
    def die[T](s: String):T = { throw new Error(s) }
    def register(name: String, t:Type): Unit = {
        if (known.contains(name)) {
            die ("Type already registered: "+name);
        }
        known = known + (name->t)
    }
    def check(ex: List[Expr], ty: List[Type]): Unit = {
        if (ex.length != ty.length) {
            die( "Length mismatch" )
        }
        if (ex.length > 0) {
            if (ex.head.isa != ty.head) {
                die( "Type mismatch" )
            }
            u.check(ex.tail, ty.tail)
        }
    }
    def equiv( x: List[Expr], y: List[Expr] ): Boolean = {
        if (x.length != y.length) return false
        var xx = x.iterator
        var yy = y.iterator
        while (xx.hasNext) {
            if (!xx.next.equiv(yy.next)) return false
        }
        return true
    }
}

/* Generic types */

class Type (name: String) {
    var arrows: HashMap[Type,FunType] = new HashMap()
    var n = 0
    def nextId(): Int = { n = n + 1; return n }

    u.register(name, this)

    def free(): FreeVar = {
        return new FreeVar(this)
    }
    def name(): String = { name }
    def from(arg: Type): FunType = {
        if (arrows.contains(arg)) {
            return arrows{arg}
        } else {
            var fun = new FunType(this, arg)
            arrows = arrows + (arg->fun)
            return fun
        }
    }
    def ret(): Type = {
        u.die ("ret called on non-function Type")
    }
    def arg(): Type = {
        u.die ("arg called on non-function Type")
    }
    override def toString(): String = { name }
}

class FunType(ret: Type, arg: Type) extends Type(ret.name+"("+arg.name+")") {
    override def ret(): Type = { ret }
    override def arg(): Type = { arg }
}

/* Generic expressions */

class Expr(isa: Type) {
    def isa(): Type = { isa }
    def stop(): Boolean = { false }
    def wanted(): List[FreeVar] = { List() }
    def canhelp(ctx: Context): Boolean = { wanted.exists( x=>ctx.vars.contains(x)) }
    def eval(): Expr = {
        eval(new Context(new HashMap()))
    }
    def eval(ctx: Context): Expr = {
        u.die ("no eval impl for "+this+" in "+this.getClass);
        this 
    }
    def eval_if_differs( unchanged: Expr, ctx: Context): Expr = {
        if (this.equiv(unchanged)) {
            println("\t[equ] skip eval of "+unchanged+"; context = "+ctx)
            return unchanged
        } else {
            return this.eval(ctx)
        }
    }
    def equiv(other: Expr): Boolean = {
        return this == other
    }
    def apply(arg: Expr): Expr = {
        isa match {
            case isa: FunType => new ApplyExpr(this, arg).check(isa.ret, new Context(new HashMap()));
            case _ => u.die("apply called on non-function value "+this+" of "+isa);
        }
    }
    def exec(ctx: Context, arg: Expr): Expr = {
        isa match {
            case isa: FunType => new ApplyExpr(this.eval(ctx), arg.eval(ctx)).check(isa.ret, ctx)
            case _          => u.die("exec called on non-function value "+this+" of "+isa)
        }
    }
    def check(t: Type, ctx: Context): Expr = {
        if (isa != t) {
            u.die("Type mismatch: expected a " +t+
                "; got value " + this + " of type "+isa)
        }
        val leftover = wanted.filter(x => ctx.vars.contains(x))
        if (leftover.length > 0) {
            u.die("Variables available in context left uninterpreted: "+leftover.mkString(","))
        }
        this
    };
    override def toString(): String = { ""+isa+"<...>" }
}

class CompoundExpr(isa: Type, deps: List[Expr]) extends Expr(isa) {
    override val wanted = deps.map( x => x.wanted ).flatten

    def deps(): List[Expr] = { deps }
    def head(): Expr = { deps.head }
    def snd():  Expr = { deps.tail.head }
    def tail(): List[Expr] = { deps.tail }
    def propagate(ctx: Context): List[Expr] = { deps.map( x => x.eval(ctx) ) }

    override def equiv(other: Expr): Boolean = other match {
        case xx: CompoundExpr => getClass == xx.getClass && isa == xx.isa &&
            u.equiv(deps, xx.deps);
        case _ => false;
    }

    override def toString(): String = { isa+"<"+deps.mkString(",")+">" }
}

class FreeVar(isa: Type) extends Expr (isa) {
    var n = isa.nextId
    override def eval(ctx: Context): Expr = {
        return ctx.getValue(this)
    }
    override def wanted(): List[FreeVar] = { List(this) }
    override def toString(): String = { ""+isa+"["+n+"]" }
    def setTo (t: Expr): Pair[FreeVar,Expr] = {
        if (t.isa != isa)
            u.die("Attempt to set "+this+" to "+t+" or type "+t.isa)
        return (this->t)   
    }
}

/* Context class - representing bound free variables */

object ContextId {
    var id = 0
    def next(): Int = { id = id + 1; return id }
}

class Context(vars: HashMap[FreeVar, Expr], parent: Int = 0) {
    var id = ContextId.next
    println( "\t[ctx] "+this )
    def vars(): HashMap[FreeVar, Expr] = { vars }
    def bind(free: FreeVar, to: Expr): Context = {
        bind( List(free), List(to) )
    }
    def bind(free: List[FreeVar], to: List[Expr]): Context = {
        var next = this
        var ifree = free.iterator
        var ito = to.iterator
        while (ifree.hasNext && ito.hasNext) {
            next = new Context(next.vars + ifree.next.setTo(ito.next), id)
        }
        if (ifree.hasNext || ito.hasNext) {
            u.die("Cannot bind lists with different length")
        }
        return next
    }
    def getValue(free: FreeVar): Expr = {
        if (vars.contains(free)) {
            return vars{free}
        }
        return free
    }
    def str(value: Expr): String = {
        value match {
            case v: FreeVar => if (vars.contains(v)) {
                    vars{v}.toString
                } else {
                    v.toString
                };
            case any => any.toString
        }
    }
    override def toString(): String = {
        "ctx["+id+"/"+parent+"]{"+vars.keys.toList.map( x => x+"="+vars{x} ).mkString(",")+"}"
    }
}

/* Applications and lambdas */

class ApplyExpr(fun: Expr, arg: Expr) extends CompoundExpr(fun.isa.ret, List(fun, arg)) {
    override def eval(ctx: Context): Expr = {
        println("\t[apl] --> apply "+fun+" to "+ctx.str(arg)+"; context="+ctx)
        var ret = arg.eval(ctx) match {
            case xx: FreeVar => new ApplyExpr(fun.eval(ctx), arg)
            case xx: Expr    => fun.exec(ctx, xx).eval_if_differs(this, ctx)
        }
        println("\t[apl] <-- got "+ret.check(fun.isa.ret, ctx)+"; context="+ctx)
        return ret
    }
    override def toString(): String = {
        "" + fun + "(" + arg + ")" 
    }
}

class Lambda(arg: FreeVar, impl: Expr) 
        extends CompoundExpr(impl.isa.from(arg.isa), List(impl, arg))
{
    var name = "lambda"
    def rename(s: String): Lambda = { name = s; this }
    override def eval(ctx: Context) = { 
        new Lambda( arg, impl.eval(ctx) )
    }
    override def toString(): String = {
        return name + "("+arg+"){"+impl+"}"
    }
    override def exec(ctx: Context, x: Expr): Expr = {
        println( "\t[lmb] --> exec "+this+" ("+x+"); context="+ctx );
        var ret = x.eval(ctx) match {
            case xx: FreeVar => new MultiargExpr (List(xx), impl)
            case xx: Expr    => impl.eval(ctx.bind(arg, xx))
        }
        println( "\t[lmb] <-- got "+ret.check(impl.isa, ctx) );
        ret
    }
}

class MultiargExpr(input: List[FreeVar], impl: Expr) extends CompoundExpr(impl.isa, impl :: input) {
    override def eval(ctx: Context): Expr = {
        println( "\t[uax] --> eval "+this+"; context="+ctx );
        var ret = impl.eval(ctx)
        println( "\t[uax] <-- got "+ret );
        ret
    }
    def getArgs(): List[FreeVar] = { input }
}

/* Partial types (aka recursive) and partial functions */
/* This is actual STLC =) */
class PartialType( name: String) extends Type(name) {
    var cons: HashMap[String,List[Type]] = new HashMap()
    def con(id: String, arg: List[Type] = List()) = {
        cons = cons + (id->arg)
        this
    }
    def getArgs(id: String): List[Type] = {
        if (!cons.contains(id)) {
            u.die("PartialType "+name+" doesn't have constructor "+id)
        }
        return cons{id}
    }
    def spawn(id: String, arg: List[Expr]): PartialExpr = {
        new PartialExpr(this, id, arg)
    }
    def spawn(id: String, arg: Expr): PartialExpr = {
        new PartialExpr(this, id, List(arg))
    }
    def spawn(id: String): PartialExpr = {
        new PartialExpr(this, id, List())
    }
}

class PartialExpr(isa: PartialType, id: String, arg: List[Expr]=List()) 
        extends CompoundExpr(isa, arg){
    u.check(arg, isa.getArgs(id))
    var to_stop = true
    arg.foreach( x => if (!x.stop) to_stop = false )

    def getArgs(): List[Expr] = { arg }
    def id(): String = { id }
    override def eval(ctx: Context): Expr = {
        new PartialExpr(isa, id, arg.map(x => x.eval(ctx)))
    }
    override def equiv(other: Expr): Boolean = other match {
        case xx: PartialExpr => xx.id == id && super.equiv(xx)
        case _ => false
    }
    override def stop(): Boolean = { to_stop }
    def depth(): Int = {
        if (arg.length == 0) {
            return 0
        }
        return arg.map( x => x match { case x: PartialExpr => x.depth; case _ => 0  } ).sortWith( _>_ ).head + 1
    }
    override def toString(): String = {
        var fin = stop() match { case true => "+"; case false => "" }
        var li  = arg.length match { 
            case 0 => ""; 
            case _ => "["+depth+"]"+"<"+arg.mkString(",")+">";
        }
        return fin+isa+"."+id+li
    }
}

class PartialFun(name: String, isa: FunType) extends Expr(isa) {
    var argtype: PartialType = isa.arg match {
        case part: PartialType => part;
        case other => u.die("Attempt to create partial function from type "+other);
    }

    var cons: HashMap[String,MultiargExpr] = new HashMap()

    def con(id: String, impl: Expr): PartialFun = {con(id, List(), impl) }
    def con(id: String, free: List[FreeVar], impl: Expr): PartialFun = {
        u.check( free, argtype.getArgs(id) )
        cons = cons + (id->new MultiargExpr(free,impl))
        this
    }
    override def eval(ctx: Context) = { this }
    override def exec(ctx: Context, arg: Expr): Expr = {
        println("\t[par] --> exec "+this+" on "+arg+"; context="+ctx )
        var ret = arg match {
            case arg: PartialExpr => {
                var impl = getImpl(arg.id)
                println("\t[par] found impl "+impl )
                impl.eval(ctx.bind(impl.getArgs, arg.getArgs))
            }
            case any => u.die("Cannot exec "+this+" on value "+any)
        }
        println("\t[par] <-- got "+ret.check(isa.ret, ctx)+"; context="+ctx )
        ret
    }
    def getImpl(id: String): MultiargExpr = {
        if (!cons.contains(id)) {
            u.die("Cannot find implementation for "+id)
        }
        cons{id}
    }
    override def toString(): String = { name+"["+isa+"]" }

}

object Smoke {
    var n = 0
    var fail: List[String] = List()
    def play(expr: Expr) {
        n = n + 1
        println( " === CASE "+n )
        try {
            println( "    "+expr )
            println( " EVALS TO" )
            var got = expr.eval
            println( "    "+got )
            if (!got.stop) fail = fail :+ ""+n+": "+expr+": non-final value "+got
        } catch {
            case e: Any => println( " *** DIED: "+e )
            fail = fail :+ ""+n+": "+expr+": died "+e
        }
        println( " === END CASE "+n )
    }

    def main(arg: Array[String]) = {
        println (" --- finite types" ); 
        var bool = new PartialType("Bool").con("true").con("false")

        var tt = bool.spawn("true")
        var ff = bool.spawn("false")

        var not = new PartialFun("not", bool.from(bool))
            .con("true", ff)
            .con("false", tt)

        play( not.apply(tt) )

        var nand = new PartialFun("nand", bool.from(bool).from(bool));
        nand.con("true", new Lambda(bool.free, ff))
        nand.con("false", not);

        play (nand.apply(ff).apply(ff))

        var switch = bool.free
        var notx = new Lambda(switch, nand.apply(switch).apply(tt));

        play (notx.apply(tt))
        play (notx.apply(ff))

        println (" --- nat (recursive)" )

        var nat  = new PartialType("Nat").con("0")
        nat.con("succ", List(nat))

        var even = new PartialFun("even", bool.from(nat))
            .con("0", tt)

        var x = nat.free
        even.con("succ", List(x), not.apply(even.apply(x)))

        var zero = nat.spawn("0")
        var one = nat.spawn("succ", zero)
        var two = nat.spawn("succ", one)
        var three = nat.spawn("succ", two)

        play(even.apply(nat.spawn("0")))
        play(even.apply(three))

        println( " ---- Some lambdas" );

        x = nat.free
        var self = new Lambda(x, x).rename("I")
        play( self.apply(three) )

        x = nat.free
        var next = new Lambda(x, nat.spawn("succ", List(x))).rename("inc")
        play( next.apply(three) )


        x = nat.free
        var k_int = new Lambda(x, new Lambda(nat.free, x)).rename("K")

        play( k_int.apply(three)(one) )

        println( " ---- Addition " );
        
        var add = new PartialFun("add", nat.from(nat).from(nat))
        add.con("0", self)


        var y = nat.free
        x = nat.free
        add.con("succ", List(x), new Lambda(y, next.apply(add.apply(x).apply(y))))
        play (add.apply(one).apply(one))


        x = nat.free
        var same = new Lambda(x, add.apply(x).apply(zero)).rename("add0")

        play( same.apply(zero) )

        x = nat.free
        var double = new Lambda( x, add.apply(x).apply(x) )

        play( double.apply(zero) );
        play( double.apply(two) );

        println( " --- now multiply " )

        var mul = new PartialFun("mul", nat.from(nat).from(nat))
        mul.con("0", new Lambda(nat.free, nat.spawn("0")))

        x = nat.free
        y = nat.free
        mul.con("succ", List(x), new Lambda(y, add.apply(y).apply(mul.apply(x).apply(y))))

        play(mul.apply(three).apply(two))

        var factorial = new PartialFun("fact", nat.from(nat)).con("0", one)
        x =nat.free
        factorial.con("succ", List(x), mul.apply(next.apply(x)).apply(factorial.apply(x)))

        println( " --- making a 4" )
        var four = add.apply(two).apply(two).eval
        println( " --- factorial!" )

        play(factorial.apply(four))

        if (fail.length > 0) {
            println( "Failed: \n"+fail.mkString("\n") )
            sys.exit(1)
        }
    } /* end main */

}
