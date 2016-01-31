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

```scala
scala> import com.localytics.{HasWDJ, WDJ}
import com.localytics.{HasWDJ, WDJ}

scala> import com.localytics.syntax.wdj._
import com.localytics.syntax.wdj._

scala> import scalaz.Semigroup
import scalaz.Semigroup

scala> import scalaz.std.list._
import scalaz.std.list._

scala> import scalaz.syntax.either._
import scalaz.syntax.either._
```

Now we define a Log type. This will serve as our Disjunction Left type,
but can also be extracted from a Right using the `wdj.logged()` method,
by way of the Wr => Lf function we must provide.

```scala
scala> case class Log(msg: String, trace: List[String])
defined class Log
```

We need a Semigroup for Log to use it as the Disjunction Left.

```scala
scala> val lappend: (Log, => Log) => Log = (l1, l2) => Log(l1.msg, l1.trace ++ l2.trace)
lappend: (Log, => Log) => Log = <function2>

scala> implicit val esg: Semigroup[Log] = Semigroup.instance(lappend)
esg: scalaz.Semigroup[Log] = scalaz.Semigroup$$anon$8@6d4e462a
```

We can now create a HasWDJ[List[String], Log] by providing a function from
List[String] => Log, and get our WDJ from it.

```scala
scala> implicit val hasWdjLSE = WDJ((ls: List[String]) => Log("success", ls))
hasWdjLSE: com.localytics.HasWDJ[List[String],Log] = com.localytics.WDJ$$anon$1@4ac87222

scala> type WDJ[A] = HasWDJ[List[String], Log]#WDJ[A]
defined type alias WDJ
```

####Basic Use

With a HasWDJ in scope, we have access to syntax enhancements
for working with the WDJ type.

Lift a known successful value of type A into a WDJ[A]
```scala
scala> 1.right[Log].toWdj[List[String]]
res0: hasWdjLSE.WDJ[Int] = EitherT(WriterT((List(),\/-(1))))
```

Lift a known failure into a WDJ[Int]
```scala
scala> Log("fubar", List()).left[Int].toWdj[List[String]]
res1: hasWdjLSE.WDJ[Int] = EitherT(WriterT((List(),-\/(Log(fubar,List())))))
```

Take two operations that may fail, appending to writer
of the WDJ and sum their successful values
```scala
scala> val x = for {
     |   a <- 1.right[Log].toWdj[List[String]].log(List("got 1"))
     |   b <- 2.right[Log].toWdj[List[String]].log(List("got 2"))
     | } yield a + b
x: scalaz.EitherT[[A]scalaz.WriterT[[+X]X,List[String],A],Log,Int] = EitherT(WriterT((List(got 1, got 2),\/-(3))))
```

Extract the logged content (Writer contents merged with Left)
```scala
scala> x.logged()
res2: Log = Log(success,List(got 1, got 2))
```

Take two operations that may fail, appending to writer of the
WDJ and sum their successful values

Note one operation fails here
```scala
scala> val y = for {
     |   a <- 1.right[Log].toWdj[List[String]].log(List("got 1"))
     |   b <- Log("failed", List()).left[Int].toWdj[List[String]].log(List("tried 2"))
     | } yield a + b
y: scalaz.EitherT[[A]scalaz.WriterT[[+X]X,List[String],A],Log,Int] = EitherT(WriterT((List(got 1, tried 2),-\/(Log(failed,List())))))
```

Extract the logged content (Writer contents merged with Left)
```scala
scala> y.logged()
res3: Log = Log(failed,List(got 1, tried 2))
```

####Bonus Features

Time an operation inside a WDJ recording the elapsed period in
the Writer using the given Double => Wr function
```scala
scala> 1.right[Log].toWdj[List[String]].mapTimed(d => List("millis-to-add-complete-"++d.toString))(_ + 3)
res4: scalaz.EitherT[[A]scalaz.WriterT[[+X]X,List[String],A],Log,Int] = EitherT(WriterT((List(millis-to-add-complete-0.0),\/-(4))))
```

Turn a List[WDJ[A]] into a tuple of (List[Lf], List[A])
Useful for accumulating failures and extracting successes
```scala
scala> List(1.right[Log].toWdj[List[String]], Log("foo", List()).left[Int].toWdj[List[String]]).tuple()
res5: (List[Log], List[Int]) = (List(Log(foo,List())),List(1))
```

What's the point?

\/ for control flow with success failure semantics

Writer for accumulation of log data

Writer means you can keep all your code side-effect free (at least as
far as monitoring/logging) and just deal with it all in one place at the
end of your code flow.

And having sugar like the mapTimed/flatMapTimed is cool too.

*  Use LDJ
*  ?
*  Profit
