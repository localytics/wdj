##WDJ

[API Docs](http://localytics.github.io/wdj/#com.localytics.package)

A monad stack.

Specifically an EitherT[Writer[Wr, \/[Lf, A]]]

A WDJ is produced by the HasWDJ[Wr, Lf] trait, which requires evidence of a
Monoid[Wr], a Semigroup[Lf] and a Wr => Lf.

Because of these requirements, the value accumulated in the Writer can be
combined with a Left value encountered by the Disjunction, which supports
behavior that is highly convenient for logging and error handling in an
application.

Let's go through an example.

####Setting up

First we need to create a HasWDJ to get our WDJ type from. This again requires
a Monoid for the Writers value and a Semigroup for the Disjunction Left.

Let's start with some imports.

```tut
import com.localytics.{HasWDJ, WDJ}
import com.localytics.syntax.wdj._
import scalaz.Semigroup
import scalaz.std.list._
import scalaz.syntax.either._
```

Now we define a Log type. This will serve as our Disjunction Left type,
but can also be extracted from a Right using the `wdj.logged()` method,
by way of the Wr => Lf function we must provide.

```tut
case class Log(msg: String, trace: List[String])
```

We need a Semigroup for Log to use it as the Disjunction Left.

```tut
val lappend: (Log, => Log) => Log = (l1, l2) => Log(l1.msg, l1.trace ++ l2.trace)
implicit val esg: Semigroup[Log] = Semigroup.instance(lappend)
```

We can now create a HasWDJ[List[String], Log] by providing a function from
List[String] => Log, and get our WDJ from it.

```tut
implicit val hasWdjLSE = WDJ((ls: List[String]) => Log("success", ls))
type WDJ[A] = HasWDJ[List[String], Log]#WDJ[A]
```

####Basic Use

With a HasWDJ in scope, we have access to syntax enhancements
for working with the WDJ type.

Lift a known successful value of type A into a WDJ[A]
```tut
1.right[Log].toWdj[List[String]]
```

Lift a known failure into a WDJ[Int]
```tut
Log("fubar", List()).left[Int].toWdj[List[String]]
```

Take two operations that may fail, appending to writer
of the WDJ and sum their successful values
```tut
val x = for {
  a <- 1.right[Log].toWdj[List[String]].log(List("got 1"))
  b <- 2.right[Log].toWdj[List[String]].log(List("got 2"))
} yield a + b
```

Extract the logged content (Writer contents merged with Left)
```tut
x.logged()
```

Take two operations that may fail, appending to writer of the
WDJ and sum their successful values

Note one operation fails here
```tut
val y = for {
  a <- 1.right[Log].toWdj[List[String]].log(List("got 1"))
  b <- Log("failed", List()).left[Int].toWdj[List[String]].log(List("tried 2"))
} yield a + b
```

Extract the logged content (Writer contents merged with Left)
```tut
y.logged()
```

####Bonus Features

Time an operation inside a WDJ recording the elapsed period in
the Writer using the given Double => Wr function
```tut
1.right[Log].toWdj[List[String]].mapTimed(d => List("millis-to-add-complete-"++d.toString))(_ + 3)
```

Turn a List[WDJ[A]] into a tuple of (List[Lf], List[A])
Useful for accumulating failures and extracting successes
```tut
List(1.right[Log].toWdj[List[String]], Log("foo", List()).left[Int].toWdj[List[String]]).tuple()
```

What's the point?

\/ for control flow with success failure semantics

Writer for accumulation of log data

Writer means you can keep all your code side-effect free (at least as
far as monitoring/logging) and just deal with it all in one place at the
end of your code flow.

And having sugar like the mapTimed/flatMapTimed is cool too.

*  Use WDJ
*  ?
*  Profit
