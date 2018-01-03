A function from `X` to `Y` may be written as a lambda term:

lambda{ x:X => f(x) }: Y

Let's define this lambda formally, using the arrow-and-dash notation:

    X,Y type
    ---------
    Expr[X=>Y] type
    lambda[X,Y]: Expr[X=>Y]->X->Y
    
A value of `Expr` is _not_ `Y` and _not_ `X->Y`. It's a completely separate type that can be _decoded_ into a function using this _lambda_ function.

It is a parametrized type. We are not yet armed to construct arbitrary parametrized types, but lets pretend this one is built into the "compiler". 

We'll write `Expr` and `lambda` in a slightly weird but compact notation:

    X,Y type
    e: Expr[X=>Y]
    --------
    lambda{ t:X => e }: X->Y
    
or equivalently (because of how arrow works):

    lambda{ t:X => e }(x): Y
    
This is exactly the Javascript idiom

    (function(x){ ... })(real_x)
    
Or Perl's `map`/`grep`/`sort` for that matter:

    map { $_*$_ } 1 .. 5;
    
Here and below, _anything_ in braces is an `Expr` and everything outside braces is a normal value. 

Just like a normal type, `Expr` has constructors and eliminators. Let's define them. 

A term of type `X` can be upgraded to `Expr[Unit=>X]`:

    x: X
    --------
    {x}: Expr[Unit => X]
    lambda{x} := x
    
Obvious so far. Just a round-trip of a pre-defined value. No big news here. 

Now let's parametrize expression with an unforeknown value of type `X`:

    x: X
    --------
    {t:X => t}: Expr[X=>X]
    lambda{ t:X => t }(x) := x

Here the `t:X` is called a *free variable of type `X`*. It *may or may not* occur on the right side of the "fat arrow" (`=>`). What *cannot* happen there is a term that is neither a pre-existing value, nor a free variable.

What we defined here is exactly the I combinator. Let's move on.

    {t:X => z}: Expr[X=>Z]
    x:X
    y:Y
    --------
    {t:X, t2:Y => z}: Expr[X*Y=>Z]
    lambda{t:X, t2:Y => z}(x)(y) := lambda{t:X => z}(x)
    {t2:Y, t:X => z}: Expr[Y*X=>Z]
    lambda{t2:Y, t:X => z}(y)(x) := lambda{t:X => z}(x)
    
Here `z` is not a variable, but arbitrary expression, however complex, being part of a correct `Expr`. The extra free variables are just discarded. Here is the K combinator in this notation:

    x: X
    y: Y
    ---------
    lambda{ t:Y => x }(y) == x

For brevity, we'll allow to add or remove any number of `u:Unit` variables on the left-hand side, having no effect on anything. We'll also assume `{ t:X*Y => z }` and `{ t.fst: X, t.snd: Y => z}` to be interchangeable, which they are, provided the right replacements are done on the right hand side. These assumptions allow us to only write down formulas for *exactly one* free variable in aech expression, even though we know there may really be any number.

Speaking of replacements,

    {t:A => z}: Expr[A=>Z]
    --------
    {t2: A => lambda{ t:A => z }(t2)}: Expr[A=>Z]
    lambda{t2: A => lambda{ t:A => z }(t2)} == lambda{ t:A => z }
    
(This can be proven by substituting any value of type A into t2).

Now let's combine two expressions in a function application. The following is exactly the S combinator:

    {t:A => x}: Expr[A=>X]
    {t:A => f}: Expr[A=>X->Y]
    a: A
    --------
    {t:A => f(x)}: Expr[A=>Y]
    lambda{t:A=>f(x)}(a) := ({t:A=>f}(a))({t:A=>x}(a))

The expression on the right of `:=` is better repeated in human words: it's the computed value of `f` applied to the computed value ot `x` for given parameter `a`. This is what one would expect when applying parametrized function to a parametrized argument!

Note that parameter `t:A` may contain a pair of *separate* parameter values for `f` and `x`, or unit for one of them, or both, or some extra variables not accounted for by neither. What can never happen is that either `f` or `x` depends on a free variable *not* contained in T. 

The last thing we need to account for is a lambda inside lambda. 
    
    { t:Y => z }:Expr[Y=>Z]
    ---------
    {t2: X => lambda{ t:Y => z}} : Expr[X=>Y->Z]
    lambda{ t2: X => lambda{ t:Y => z}} == lambda{ t2:X, t:Y => z }

And this allows us to build arbitrarily complex functions from constructors, eliminators, and previously defined functions.

**Ex.1:**

    f: (X*Y)->Z
    --------
    g(y,x) := lambda{ t1:Y, t2:X => f(t2,t1) }
    g(y,x) == f(x,y)
    
**Ex.2:**

    f: Y->Z
    g: X->Y
    --------
    fg: X->Z
    fg = lambda{ t:X => f(g(t)) }    
    
**Ex.3:**

callback

    f: X->Y
    ---------
    lambda{ t:X, callback:Y->Z => callback(f(t)) }: X->Z
    