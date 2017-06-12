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

# Enumerable (finite) types

Just like above, we can define a type with any number of predefined values.
We can also give them names for readability.







