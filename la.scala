package stlc.la;

import scala.collection.immutable.HashMap;

class Error(s: String) extends Exception(s) {
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
}

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

class Expr(isa: Type) {
    def isa(): Type = { isa }
    def eval(): Expr = {
        eval(new Context(new HashMap()))
    }
    def eval(ctx: Context): Expr = {
        u.die ("no eval impl for "+this+" in "+this.getClass);
        this 
    }
    def apply(arg: Expr): Expr = {
        isa match {
            case isa: FunType => new ApplyExpr(this, arg);
            case _ => u.die("apply called on non-function value "+this+" of "+isa);
        }
    }
    def exec(ctx: Context, arg: Expr): Expr = {
        u.die("apply called on non-function value "+this+" of "+isa)
    }
    override def toString(): String = { ""+isa+"<...>" }
}

class ApplyExpr(fun: Expr, arg: Expr) extends Expr(fun.isa.ret) {
    override def eval(ctx: Context): Expr = {
        println("\t[apl] --> apply "+fun+" to "+ctx.str(arg)+"; context="+ctx)
        var ret = fun.eval(ctx).exec(ctx, arg.eval(ctx)).eval(ctx)
        println("\t[apl] <-- got "+ret+"; context="+ctx)
        return ret
    }
    override def toString(): String = {
        "" + fun + "(" + arg + ")" 
    }
}

object ContextId {
    var id = 0
    def next(): Int = { id = id + 1; return id }
}

class Context(vars: HashMap[FreeVar, Expr], parent: Int = 0) {
    var id = ContextId.next
    println( "\t[ctx] "+this )
    def bind(free: FreeVar, to: Expr): Context = {
        if (free.isa != to.isa) {
            u.die("Cannot bind var of type "+free.isa+" to value "+to)
        }
        return new Context( vars + (free->to), id )
    }
    def bind(free: List[FreeVar], to: List[Expr]): Context = {
        if (free.length != to.length) {
            u.die("Cannot bind lists with different length")
        }
        if (free.length == 0) {
            return this
        }
        bind(free.head, to.head).bind(free.tail, to.tail)
    }
    def getValue(free: FreeVar): Expr = {
        if (!vars.contains(free)) {
            return free
        }
        return vars{free}
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

class FreeVar(isa: Type) extends Expr (isa) {
    var n = isa.nextId
    override def eval(ctx: Context): Expr = {
        return ctx.getValue(this)
    }
    override def toString(): String = { ""+isa+"["+n+"]" }
}

class FunType(ret: Type, arg: Type) extends Type(ret.name+"("+arg.name+")") {
    override def ret(): Type = { ret }
    override def arg(): Type = { arg }
}

class MultiargExpr(input: List[FreeVar], impl: Expr) extends Expr(impl.isa) {
    override def eval(ctx: Context): Expr = {
        impl.eval(ctx)
    }
    def getArgs(): List[FreeVar] = { input }
}

/* ============= */

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
        extends Expr(isa){
    u.check(arg, isa.getArgs(id))
    def getArgs(): List[Expr] = { arg }
    def id(): String = { id }
    override def eval(ctx: Context): Expr = {
        new PartialExpr(isa, id, arg.map(x => x.eval(ctx)))
    }
    override def toString(): String = {
        if (arg.length > 0) {
            return ""+isa+"."+id+"<"+arg.mkString(",")+">"
        } else {
            return ""+isa+"."+id
        }
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
        arg match {
            case arg: PartialExpr => {
                var impl = getImpl(arg.id)
                println("\t[par] --> apply "+impl+" to "+arg+"; context="+ctx )
                var ret = impl.eval(ctx.bind(impl.getArgs, arg.getArgs))
                println("\t[par] <-- got "+ret+"; context="+ctx )
                ret
            }
            case any => u.die("Cannot apply "+this+" to value "+any)
        }
    }
    def getImpl(id: String): MultiargExpr = {
        if (!cons.contains(id)) {
            u.die("Cannot find implementation for "+id)
        }
        cons{id}
    }
    override def toString(): String = { name+"["+isa+"]" }

}

class Lambda(arg: FreeVar, impl: Expr) 
        extends Expr(impl.isa.from(arg.isa))
{
    override def eval(ctx: Context) = { 
        new Lambda( arg, impl.eval(ctx) )
    }
    override def toString(): String = {
        return "lambda("+arg+"){"+impl+"}"
    }
    override def exec(ctx: Context, x: Expr): Expr = {
        println( "\t[lmb] --> exec "+this+" ("+x+"); context="+ctx );
        var ret = impl.eval(ctx.bind(arg, x.eval(ctx)))
        println( "\t[lmb] <-- got "+ret+"; context="+ctx );
        ret
    }
}

object Smoke {
    var n = 0
    def play(expr: Expr) {
        n = n + 1
        println( " === CASE "+n )
        try {
            println( "    "+expr )
            println( " EVALS TO" )
            println( "    "+expr.eval() )
        } catch {
            case e: Any => println( " *** DIED: "+e )
        }
        println( " === END CASE "+n )
    }

    def main(arg: Array[String]) = {
        var bool = new PartialType("Bool").con("true").con("false")

        var nat  = new PartialType("Nat").con("0")
        nat.con("succ", List(nat))

        var not = new PartialFun("not", bool.from(bool))
            .con("true", bool.spawn("false"))
            .con("false", bool.spawn("true"))

        var even = new PartialFun("even", bool.from(nat))
            .con("0", bool.spawn("true"))

        var x = nat.free
        even.con("succ", List(x), not.apply(even.apply(x)))

        var zero = nat.spawn("0")
        var one = nat.spawn("succ", zero)
        var two = nat.spawn("succ", one)
        var three = nat.spawn("succ", two)

        var f1 = not.apply(bool.spawn("true"))

        play(f1);
        play(even.apply(nat.spawn("0")))
        play(even.apply(three))

        println( " ---- Some lambdas" );

        x = nat.free
        var self = new Lambda(x, x)
        play( self.apply(three) )

        x = nat.free
        var next = new Lambda(x, nat.spawn("succ", List(x)))
        play( next.apply(three) )


        x = nat.free
        var k = new Lambda(x, new Lambda(nat.free, x))

        play( k.apply(three)(one) )

        println( " ---- Addition " );
        
        var add = new PartialFun("add", nat.from(nat).from(nat))
        add.con("0", self)


        var y = nat.free
        x = nat.free
        add.con("succ", List(x), new Lambda(y, next.apply(add.apply(x).apply(y))))
        play (add.apply(one).apply(three))

    }

}
