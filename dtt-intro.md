# A GENTLE INTRODUCTION TO DEPENDENT TYPE THEORY

# Foreword

This work is going to explain some basics of Simply Typed Lambda Calculus (STLC)
and Dependent Type Theory (DTT).

It does not require serious mathematical background,
although exposure to calculus, linear algebra, group theory, set theory,
computational theory and practical programming is desired.


# The judgement formalism

In order to write down our axioms and definitions,
we're going to use simple yet powerful notation based on judgements.
There are four types of them:

## Type judgement

    Expr type

This means that `Expr` is a *type*, although we do not define what a *type* is.

## Value judgement

    expr: Expr

This means that `expr` is a *value*, or specimen, of *type* `Expr`.
Just like with sets/elements, we can tell for sure if a given value belongs
to a certain type.
We do *not* rely on set theory (either naive or ZF\*) though.

To avoid confusion, we'll always start type names with capital letters,
and value names with lowercase letters.

## Uniqueness (eta-rule)

    expr1 == expr2

For two values of the same type, this establishes the fact that in any
statement, `expr1` and `expr2` are interchangeable.


## Computation (beta-rule)

    expr1 := expr2

For two values of the same type, this establishes the fact that the left side
value should be replaced with the right one (but *not* vice-versa) in  order
to evaluate a formula.

To avoid confusion, we are *not* going to use a single equal sign anywhere below.

## The long dash

In a group of judgements, we'll separate the *premise* from the *consequence*
with a long horisontal line:

     T type
     ---------------
     T->T type
     i: T->T

     T type 
     x: T
     ---------------
     i(x) := x
   
This defines an identity function for any given type T.
It's also called the I-combinator in combinatory logic.

# Functions: the allmighty arrow

Before we have at least one type with at least one value,
let's define the mechanisms for combining existing types into new ones.

First and foremost, we shall define a *function*:

    A type 
    Z type
    ----------------
    A->Z type

    f: A->Z
    a: A
    ----------------
    f(a): Z

    f: A->Z
    a,b: A
    a == b
    ----------------
    f(a) == f(b)

We hereby postulate that 

* For two given types, a type of functions from one to the other also exists.
* For *any* value of first (argument) type, a function value exists
and belongs to the second (return) type.
* For any given arguments, return equality follows from argument equality.

Despite some serious limitations (the function must be **total**,
and it must be **pure** at least to the point of *detectable* side effects),
we do *not* state anything about its nature.

E.g. we do *not* demand computability with a halting Turing machine.
We do not yet have a powerful enough theory to define what a Turing machine is.

One may view functions as an interface rather than a class of objects.
All we know is that an `f:A->B` converts values of `A` into `B`,
in a predictable manner.

## Functions of multiple arguments

A function of several arguments `f(a,b...)` may be viewed as a function
of `a` returning a new function of the rest of arguments parametrized with a
(a trick discovered by Huskell Curry):

    f(a,b,...) = f(a)(b,...)

Or, in the dash motation:

    f: A->(B->Z)
    a: A
    b: B
    ----------------
    f(a,b): Z
    f(a,b) := f(a)(b)

Below here, we'll interpret `A->B->Z` (w/o parentheses) as `A->(B->Z)`,
i.e. a multiple argument function, and not a function converting
`A->B` into a value of `Z`.

# Union of types

    A type
    B type
    ----------------
    A+B type

For any two given types, a *union* or *sum* type exists.

    a: A
    B type
    ----------------
    inl(a): A+B

    b: B
    A type
    ----------------
    inr(b): A+B

For any value of *both* initial types, a corresponding value in the union exists.

Now we need to ensure nothing gets into the union except the abovementioned
values.
We do so by defining a powerful `match` operation.
In fact, it is just an `if` statement, written in our dash notation:

    (A+B)->Z
    ----------------
    match: (A->Z)->(B->Z)->((A+B)->Z)
    match(f,g)(inl(a)) := f(a)
    match(f,g)(inr(b)) := g(b)

And hereby we postulate that *any* function from a union 
(or its indistinguishable analog)
can be reconstructed
using a match and a suitable pair of functions:

    f: (A+B)->Z
    ----------------
    f_a: A->Z
    f_b: B->Z
    f == match(f_a, f_b)

Due to the totality property discussed above, this implies that all values in
the union may indeed be handled by match and therefore belong to either A or B.

Like before, an `A+B+C` notation denotes `A+(B+C)` as this is the direction
in which we would traverse an `if ... else if ... else` statement.

# An ordered pair, or record

This is actually *not* required for building the following theory
(plus and arrow are sufficient).
However, combining values to form a compound value is quite natural.
Therefore, for convenience pairs are also listed here as a basic type
generation method.

    A type
    B type
    ----------------
    A*B type
    pair: A->B->(A*B)
    fst: A*B->A
    snd: A*B->B

For any given two types A and B, a type pair(A,B) also exists
and has two functions `fst` and `snd` which return A and B, respectively.
The concrete definition of fst and snd is rather straightforward:

    a: A
    b: B
    ----------------
    fst(pair(a,b)) := a
    snd(pair(a,b)) := b

Again, we add an uniqueness rule to guarantee that nothing else
exists in the pair type:

    p: A*B
    ----------------
    p == pair(fst(p),snd(p))

Just like above, a `A*B*C` type should be interpreted as `A*(B*C)` and is,
indeed, a tuple with more than two fields.

Anywhere below here, the `*` operation has a higher priority than `+`,
and `+` a higher priority than `->`. This is just for convenience.
Parentheses are used if needed.

Replacing `fst(snd(....))` with human-readable names further improves
convenience, leaving us with a simple *record* or object notation.

## Tuples and currying

Quite naturally a two-argument function discussed above 
and a function of pair are
interchargeable with the right syntactic sugar:

    A*B,Z:type
    ----------------
    curry: ((A*B)->Z)->A->B->Z
    uncurry: (A->B->Z)->(A*B)->Z

    a: A
    b: B
    f: A*B->Z
    g: A->B->Z
    ----------------
    curry(f)(a)(b) := f(pair(a,b))
    uncurry(g)(pair(a,b)) := g(a)(b)

# The unit type

Now that we have some type extention tooling, let's finally construct some
actual types with values.

The `unit` (void, 1) type is very simple. It only has one value:

    ()
    ----------------
    Unit type
    u: Unit

    a: Unit
    b: Unit
    ----------------
    a == b

Note that storing a unit value requires 0 bits, not 1!

The arrows to and from unit are quite simple.

    T type
    ----------------
    discard: T->Unit

That is, `discard` a value. It's the only function from any type into `Unit`,
modulo side effects (which we currently don't have anyway).

    T type
    t: T
    ----------------
    return t: Unit->T
    (return t)(u) := t

Looks silly, but it is going to be a useful building block for
meaningful functions.

# The none (null) type

Not to be confused with Unit, the None type (or null, undef, etc) does not
have any values at all.
Since this theory is constructive, there is no simple way to write it down.
We will exploit the fact that *any* statement (including a false one)
follows from a false premise.

    ()
    ---------------
    None type

    x: None
    ---------------
    any statement

As follows from the arrow definition,

    f: X->None
    x: X
    ----------------
    f(x): None
    and therefore any statement

Which means that existence of a function from `X` to `None` turns `X` to `None`.
We'll use that later to restrict dependent types via negation.

Curiously enough, `A+None` is exactly `A` and `A*None` is exactly `None`
since we won't find a second element for the pair no matter how good
the first one is.

# Boolean - finally a nontrivial type

    ()
    ---------------
    Boolean := Unit + Unit

This definition ensures a Boolean may only have 2 values, to which we'll give
names for convenience:

    unit: Unit
    ---------------
    true: Boolean
    true := inr(unit)
    false: Boolean
    false := inl(unit)

Now finally this type is a bit of information.

A match operator described above will have a very specific form for a boolean:

    f: Boolean->T
    ---------------
    f_true: Unit->T
    f_false: Unit->T
    f = match(t_true, t_false)

And therefore, since we know how `Unit->T` looks

    f: Boolean->T
    ---------------
    t1: T
    t2: T
    f = match(return t1, return t2)

And this is exactly how *all* functions from `Boolean->T` look!
You may think of this as C's ternary operator: `condition ? value1 : value2;`

# Enumerable (finite) types

Just like above, we can define a type with any number of predefined values.
We can also give them names for readability.

And similarly to boolean, a function with enumerable argument
is going to look like

    f: "x1"+"x2"+...+"xn"->T
    ---------------
    t1: T
    t2: T
    ...
    tn: T
    f == match(return t1, match( return t2, match( ..., return tn )...))

Or it may be viewed as an array of values, a finite list of pairs,
a hash table, etc.

Such table may also be represented as a tuple:

    T,Z type
    f: Unit+T->Z
    ---------------
    t: Z*(T->Z)
    f == match(return t.fst, t.snd)

Note that `return` keyword here serves to convert concrete value (`t.fst`)
into a function (`Unit->Z`),
while `t.snd` is already a function by itself and we do *not* intend
to return it as a value.

Similarly, a tuple may be reduced to a sum of types:

    T,Z type
    ---------------
    (Unit+T)*Z == Z+T*Z

Looks similar to how normal product is defined.

This may lead us to an idea that a function from and to finite types is
itself a finite type. And this is right! But our theory is not strong enough
for a formal proof yet.

Indeed, only arbitrarily large but finite types and values may be constructed
from Unit using the `+`, `*`, and `->` operators.

<exercise>Starting at Boolean, construct a type that would be enough
to enumerate all protons in the known universe.</exercise>

# Natural numbers?

The sad truth is that finite types only allow us to express the facts
we already know.
To break free, we'll need to modify the theory itself.

Let's look closely at the `Unit+T` type mentioned above. As you can see, 
it boils down to 1+1+1+...+1. Can we get rid of the last "1" and just leave
the dots there?

Well, we can (informally):

    ()
    --------------
    Nat type
    Nat == "0" + Nat

Huh? Well, a natural number is indeed either a starting value (typically 1
but starting at 0 is also possible), or a successor of another natural number.

Let's try to restate that using inr() and inl(), just as we did for union:

    ()
    -------------
    Nat type
    inl("0"): Nat
    inr(n: Nat): Nat

The *new* thing here is that Nat may depend on itself and not *another* type.

A function can now be expressed (just as with `A+B->T`) as a match:

    f: Nat->T
    -------------
    t0: T
    repeat: Nat->T
    f == match( return t0, repeat )

Note that the argument to repeat(T) will always be the *previous* value.
Still a function of natural argument depends on another function of natural
argument.
On the other hand, we may still be sure that a calculation ends, provided that
the chain of extractions reaches "0" at some point.

# Recursive types

Type T is a recursive type if and only if it can be represented as a union
of named constructors depending on other types, including T, *unless*
T (or anything depending on T) is on the left side of an arrow.

A function of a recursive type is defined as a match of its constructors.
For readability, we'll replace positional match with a named one:

    A,B,C,Z type
    T type (recursive)
    a(A): T
    b(B): T
    c(C): T
    fa: A->Z
    fb: B->Z
    fc: C->Z
    ---------------
    match( a => f1, b => f2, c => f3 ) == match( f1, match( f2, f3 ))

Unions, pairs, and all enumerable types are recursive types by this definition.
Unit is not recursive (it has a special condition regarding its values).
Arrows are not recursive (we don't know at all how to construct them
at this point).

Now we can formally define Nat:

    ()
    ---------------
    Nat type (recursive)
    "0": Nat
    succ(n: Nat): Nat

Now to work with these numbers we must specify how we treat 0's and how
we treat a number's successor:

    f: Nat->T
    --------------
    t0: T
    repeat: Nat->T
    f == match( "0" => return t0; succ(n) => repeat(n) )

And of course `repeat` may itself contain `f`, leading to (tadam) recursion.

However, such definition may lead to *infinite* recursion which is not
what we want yet. And this happens because of `Nat` on the left side
of an arrow.

    f: Nat->Context
    -------------
    t0: Context
    repeat: Context->Context
    f == match( "0" => return t0; succ(n) => return repeat(f(n)) )

Such `f` will halt, provided that `repeat` has no references to `f` and 
`Context` is itself recursive.

We call the resulting type `Context` because it may enclose temporary variables
as well as the desired value. If we need a value of type T, we may assume
Context to be `T*Vars` and thus

    f: Nat->T
    --------------
    f': Nat->T*Vars
    f(n) = f'(n).fst

<exercise>Implement n! (the product of all naturals from 1 to n).</exercise>

# Lists

# lamdba terms

# Combinators

What if we allow a recursive type on the left side of an arrow?

Here's an example.

    ()
    --------------
    T type (recursive)
    fun(T->T): T

T has only one constructor, therefore functions on it may only be defined as
a match with one branch:

    T type (...)
    --------------
    i: T->T
    i(fun(t)) := fun(t)
    k: T->T
    k(fun(t))(fun(unused)) := fun(t)
    s: T->T
    s(fun(f))(fun(g))(fun(arg)) := fun(f(arg)(g(arg)))

This structure is known as combinatory logic.
It's known to be Turing complete, and thus may include functions that
never return.

# Peano Arithmetic

How potent are recursive types with the limitation?
It may be shown that `Nat` with `+`, `*`, and `->` is as powerful as
Peano arithmetic and so is allowing *all* recursive types.

And this is what constitutes simply typed lambda calculus.

# Universes

We can create natural numbers. What about even numbers or prime numbers?
These can be addressed, too, but a lot of work is needed to do so.

Let's first create a Universe (BANG!).

In ordinary set theory (both naive and ZF) sets may belong to larger sets.
The types, however, do not belong to other types.

We've seen types parametrized with other types.
What about types parametrized with values?
Here we go:

    ()
    -------------
    U type
    u: U
    Dec(u) type

Ouch. 
