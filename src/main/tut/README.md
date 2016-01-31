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

```tut
import com.localytics.{HasWDJ, WDJ}
import com.localytics.syntax.wdj._
import scalaz.Semigroup
import scalaz.std.list._
import scalaz.syntax.either._

// A simple log type with a message and trace
case class Log(msg: String, trace: List[String])

// A Semigroup for the error type
val lappend: (Log, => Log) => Log =
  (l1, l2) => Log(l1.msg, l1.trace ++ l2.trace)
implicit val esg: Semigroup[Log] = Semigroup.instance(lappend)

// Create a HasWDJ[List[String], Log] by providing a function from
// List[String] => Log
implicit val hasWdjLSE = WDJ((ls: List[String]) => Log("", ls))

type WDJ[A] = HasWDJ[List[String], Log]#WDJ[A]

// Lift a known successful value of type A into a WDJ[A]
1.right[Log].toWdj[List[String]]

// Lift a known failure into a WDJ[Int]
Log("fubar", List()).left[Int].toWdj[List[String]]

// Take two operations that may fail, appending to writer
// of the LDJ and sum their successful values
val x = for {
  a <- 1.right[Log].toWdj[List[String]].log(List("got 1"))
  b <- 2.right[Log].toWdj[List[String]].log(List("got 2"))
} yield a + b

// Extract the logged content (Writer contents merged with Left)
x.logged()

// Take two operations that may fail, appending to writer of the
// LDJ and sum their successful values

// Note one operation fails here
val y = for {
  a <- 1.right[Log].toWdj[List[String]].log(List("got 1"))
  b <- Log("failed", List()).left[Int].toWdj[List[String]].log(List("tried 2"))
} yield a + b

// Extract the logged content (Writer contents merged with Left)
y.logged()

//Bonus round

// Time an operation inside a DJW recording the elapsed period in
// the Writer using the given Double => Wr function
1.right[Log].toWdj[List[String]].mapTimed(d => List("millis-to-add-3"++d.toString))(_ + 3)

// Turn a List[WDJ[A]] into a tuple of (List[Lf], List[A])
// Useful for accumulating failures and extracting successes
List(1.right[Log].toWdj[List[String]], Log("foo", List()).left[Int].toWdj[List[String]]).tuple()
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
