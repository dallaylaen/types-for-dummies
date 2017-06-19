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
    def free(): FreeVar = {
        return new FreeVar(this)
    }
    def name(): String = { name }
    def from(arg: Type): FunType = {
        new FunType(this, arg)
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
    def eval(ctx: Context = new Context()): Expr = { this }
    def apply(arg: Expr): Expr = {
        u.die("apply called on non-function value")
    }
    def exec(ctx: Context, arg: Expr): Expr = {
        u.die("apply called on non-function value")
    }
    override def toString(): String = { ""+isa+"<...>" }
}

class Context(vars: HashMap[FreeVar, Expr] = HashMap()) {
    def bind(free: FreeVar, to: Expr): Context = {
        if (free.isa != to.isa) {
            u.die("Cannot bind var of type "+free.isa+" to value "+to)
        }
        return new Context( vars + (free->to) )
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
            u.die("Cannot find value in context")
        }
        return vars{free}
    }
}

class FreeVar(isa: Type) extends Expr (isa) {
    override def eval(ctx: Context): Expr = {
        return ctx.getValue(this)
    }
    override def toString(): String = { ""+isa+"<?>" }
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
    def spawn(id: String, arg: List[Expr] = List()): PartialExpr = {
        new PartialExpr(this, id, arg)
    }
}

class PartialExpr(isa: PartialType, id: String, arg: List[Expr]=List()) 
        extends Expr(isa){
    u.check(arg, isa.getArgs(id))
    def getArgs(): List[Expr] = { arg }
    def id(): String = { id }
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
    override def exec(ctx: Context, arg: Expr): Expr = {
        arg match {
            case arg: PartialExpr => {
                var impl = getImpl(arg.id)
                impl.eval(ctx.bind(impl.getArgs, arg.getArgs))
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

    /* TODO this should go to generic FunExpr */
    override def apply(arg: Expr): Expr = {
        if (argtype != arg.isa) {
            u.die("Cannot apply "+this+" to value "+arg)
        }
        return new ApplyExpr(this, arg)
    }
}

class ApplyExpr(fun: Expr, arg: Expr) extends Expr(fun.isa.ret) {
    override def eval(ctx: Context): Expr = {
        fun.eval(ctx).exec(ctx, arg.eval(ctx)).eval(ctx)
    }
    override def toString(): String = {
        "" + fun + "(" + arg + ")" 
    }
}

object Smoke {
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

        var three = nat.spawn("0")
        three = nat.spawn("succ", List(three))
        three = nat.spawn("succ", List(three))
        three = nat.spawn("succ", List(three))

        var f1 = not.apply(bool.spawn("true"))

        play(f1);
        play(even.apply(nat.spawn("0")))
        play(even.apply(three))
    }

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
}
