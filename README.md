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

Here are some basic examples:

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

scala> // A simple log type with a message and trace
     | case class Log(msg: String, trace: List[String])
defined class Log

scala> // A Semigroup for the error type
     | val lappend: (Log, => Log) => Log =
     |   (l1, l2) => Log(l1.msg, l1.trace ++ l2.trace)
lappend: (Log, => Log) => Log = <function2>

scala> implicit val esg: Semigroup[Log] = Semigroup.instance(lappend)
esg: scalaz.Semigroup[Log] = scalaz.Semigroup$$anon$8@25df2ec5

scala> // Create a HasWDJ[List[String], Log] by providing a function from
     | // List[String] => Log
     | implicit val hasWdjLSE = WDJ((ls: List[String]) => Log("", ls))
hasWdjLSE: com.localytics.HasWDJ[List[String],Log] = com.localytics.WDJ$$anon$1@6f3e2cff

scala> type WDJ[A] = HasWDJ[List[String], Log]#WDJ[A]
defined type alias WDJ

scala> // Lift a known successful value of type A into a WDJ[A]
     | 1.right[Log].toWdj[List[String]]
res5: hasWdjLSE.WDJ[Int] = EitherT(WriterT((List(),\/-(1))))

scala> // Lift a known failure into a WDJ[Int]
     | Log("fubar", List()).left[Int].toWdj[List[String]]
res7: hasWdjLSE.WDJ[Int] = EitherT(WriterT((List(),-\/(Log(fubar,List())))))

scala> // Take two operations that may fail, appending to writer
     | // of the LDJ and sum their successful values
     | val x = for {
     |   a <- 1.right[Log].toWdj[List[String]].log(List("got 1"))
     |   b <- 2.right[Log].toWdj[List[String]].log(List("got 2"))
     | } yield a + b
x: scalaz.EitherT[[A]scalaz.WriterT[[+X]X,List[String],A],Log,Int] = EitherT(WriterT((List(got 1, got 2),\/-(3))))

scala> // Extract the logged content (Writer contents merged with Left)
     | x.logged()
res11: Log = Log(,List(got 1, got 2))

scala> // Take two operations that may fail, appending to writer of the
     | // LDJ and sum their successful values
     | 
     | // Note one operation fails here
     | val y = for {
     |   a <- 1.right[Log].toWdj[List[String]].log(List("got 1"))
     |   b <- Log("failed", List()).left[Int].toWdj[List[String]].log(List("tried 2"))
     | } yield a + b
y: scalaz.EitherT[[A]scalaz.WriterT[[+X]X,List[String],A],Log,Int] = EitherT(WriterT((List(got 1, tried 2),-\/(Log(failed,List())))))

scala> // Extract the logged content (Writer contents merged with Left)
     | y.logged()
res17: Log = Log(failed,List(got 1, tried 2))

scala> //Bonus round
     | 
     | // Time an operation inside a DJW recording the elapsed period in
     | // the Writer using the given Double => Wr function
     | 1.right[Log].toWdj[List[String]].mapTimed(d => List("millis-to-add-3"++d.toString))(_ + 3)
res22: scalaz.EitherT[[A]scalaz.WriterT[[+X]X,List[String],A],Log,Int] = EitherT(WriterT((List(millis-to-add-30.0),\/-(4))))

scala> // Turn a List[WDJ[A]] into a tuple of (List[Lf], List[A])
     | // Useful for accumulating failures and extracting successes
     | List(1.right[Log].toWdj[List[String]], Log("foo", List()).left[Int].toWdj[List[String]]).tuple()
res25: (List[Log], List[Int]) = (List(Log(foo,List())),List(1))
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
