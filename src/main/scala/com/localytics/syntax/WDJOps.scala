package com.localytics.syntax

import com.localytics.HasWDJ
import scala.language.existentials
import scala.language.higherKinds
import scala.language.implicitConversions
import scalaz.{\/, Monoid, Semigroup, Writer}
import scalaz.syntax.std.boolean._
import scalaz.syntax.either._
import scalaz.syntax.id._
import scalaz.syntax.monoid._
import scalaz.syntax.writer._

object wdj {

  implicit class DjToWDJ[Lf, A](dj: \/[Lf, A]) {
    /**
     * Lifts a \/[Lf, A] into a WDJ[A] given evidence of a
     * HasWDJ[Wr, Lf].
     */
    def toWdj[Wr](implicit hw: HasWDJ[Wr, Lf]): hw.WDJ[A] = hw.djToWdj(dj)
  }

  implicit class VToW[Lf, Wr, A](v: HasWDJ[Wr, Lf]#V[A])(
                        implicit _hw: HasWDJ[Wr, Lf]) {
    val hw = _hw
    /**
     * Lifts a Writer[Wr, \/[Lf, A]] into a WDJ[A] given evidence of
     * a HasWDJ[Wr, Lf].
     */
    def toWdj: hw.WDJ[A] = hw.vToWdj(v)
  }

  class UsesWDJOps[Wr, Lf](implicit hw: HasWDJ[Wr, Lf]) {
    type WDJ[A] = HasWDJ[Wr, Lf]#WDJ[A]
    implicit val wrM = hw.wrM
    implicit val lfSg = hw.lfSg
    implicit def wrToLf = hw.wrToLf
    implicit def vToWdj[A](v: HasWDJ[Wr, Lf]#V[A]): WDJ[A] = hw.vToWdj(v)
    implicit def djToWdj[A](dj: \/[Lf, A]): WDJ[A] = hw.djToWdj(dj)
  }

  implicit class ToWdjOps[A](v: A) {

    /**
     * Lifts an A into a WDJ[A] given evidence of a
     * HasWDJ[Wr, Lf] for specified Wr and Lf types.
     * The Monoid[Wr].zero will be used to initialize the
     * contents of the Writer in the stack.
     */
    def rightWdj[Wr, Lf](implicit hw: HasWDJ[Wr, Lf]):
    HasWDJ[Wr, Lf]#WDJ[A] =
      hw.vToWdj(v.right[Lf].set(hw.wrM.zero))

    /**
     * Lifts an A into a WDJ[B] given evidence of a
     * HasWDJ[Wr, A] for specified Wr type.
     * The Monoid[Wr].zero will be used to initialize the
     * contents of the Writer, and the Disjunction will
     * be a Left
     */
    def leftWdj[Wr, B](implicit hw: HasWDJ[Wr, A]):
    HasWDJ[Wr, A]#WDJ[B] =
      hw.vToWdj(v.left[B].set(hw.wrM.zero))
  }

  implicit class WdjOps[A, Wr, Lf](wa: HasWDJ[Wr, Lf]#WDJ[A])(
                                  implicit hw: HasWDJ[Wr, Lf])
    extends UsesWDJOps[Wr, Lf] {

    /**
     * Transform this WDJ[A] to a HasWDJ[Wr2, Lf2]#WDJ[A] using
     * the provided Wr => Wr2 and Lf => Lf2 functions, given evidence
     * of a HasWDJ[Wr2, Lf2] for the specified Wr2 and Lf2 types.
     */
    def transform[Wr2, Lf2](wt: Wr => Wr2)(lt: Lf => Lf2)(
                    implicit hw2: HasWDJ[Wr2, Lf2]): HasWDJ[Wr2, Lf2]#WDJ[A] =
      wa.run.value |> { v =>
        v.map(x => new ToWdjOps(x).rightWdj(hw2))
         .getOrElse(new ToWdjOps(lt(v.swap.toOption.get)).leftWdj[Wr2, A])
         .log(wt(wa.run.written))
      }

    /**
     * Collect the contents of the Writer and Left values together from
     * this WDJ using the provided Wr => Lf on the matching HasWDJ[Wr, Lf],
     * along with the Semigroup[Lf] in the case this WDJ contains a Left value.
     *
     * An optional Wr can be provided to be appended to the Writer contents
     * if modification of resulting value is required.
     */
    def logged(merge: Wr = wrM.zero): Lf =
      (wrToLf(wa.run.written |+| merge) |> { ll: Lf =>
        wa.fold(_|+|ll, _ => ll)
      }).run._2

    /**
     * Append the given Wr to the Writer contents of this WDJ using
     * the given Monoid[Wr] from the matching HasWDJ[Wr, Lf]
     */
    def log(w: Wr): WDJ[A] =
      wa.run.flatMap(_.set(w))

    def withTimer[B](tfn: Double => Wr, fn: WDJ[A] => WDJ[B]): WDJ[B] = {
      val st = System.currentTimeMillis()
      fn(wa)
        .log(tfn(System.currentTimeMillis().toDouble - st.toDouble))
    }

    /**
     * flatMap the provided fn over this WDJ, recording the milliseconds
     * taken to complete in the Writer value using the provided
     * Double => Wr function
     */
    def flatMapTimed[B](tfn: Double => Wr)(fn: A => WDJ[B]): WDJ[B] =
      withTimer(tfn, (_: WDJ[A]).flatMap(fn))

    /**
     * map the provided fn over this WDJ, recording the milliseconds
     * taken to complete in the Writer value using the provided
     * Double => Wr function
     */
    def mapTimed[B](tfn: Double => Wr)(fn: A => B): WDJ[B] =
      withTimer(tfn, (_: WDJ[A]).map(fn))

    /**
     * Perform a given side effect on the Left value of this WDJ,
     * returning the original WDJ.
     */
    def tapLeft(f: Lf => Unit): WDJ[A] =
      wa.leftMap { _ <| f }

    /**
     * Get the Right value of this WDJ or return the provided alternative
     */
    def wGetOrElse(a: A): A = wa.run.value.getOrElse(a)

    /**
     * Returns a tuple of (Lf, A) containing the
     * successful value of this WDJ or the provided alternative if
     * this W is a -\/
     */
    def tuple(orElse: A, wr: Wr = wrM.zero): (Lf, A) =
      wa.map((wa.logged(wr), _))
        .wGetOrElse((wa.logged(wr), orElse))

    /**
     * Returns a tuple of (Lf, Option[A])
     *
     * Lf will pertain to both Left or Right values using the
     * result of the WDJ.logged() method to populate.
     * Left values will produce None while Right values will
     * produce Some[A] in the second position of the resulting tuple.
     */
    def tupleO(wr: Wr = wrM.zero): (Lf, Option[A]) =
      wa.map(a => (wa.logged(wr), (Some(a): Option[A])))
        .wGetOrElse((wa.logged(wr), (None: Option[A])))
  }

  implicit class ListWOps[A, Wr, Lf](lle: List[HasWDJ[Wr, Lf]#WDJ[A]])(
                            implicit _hwa: HasWDJ[Wr, Lf])
    extends UsesWDJOps[Wr, Lf] {

    /**
     * Splits a list of WDJ[A] into a tuple of (List[Lf], List[A])
     */
    def tuple(z: List[Lf] = List()): (List[Lf], List[A]) =
      lle.foldRight((z, List.empty[A]))((a, t) =>
        a.fold(_ => ((a.logged() :: t._1), t._2),
               v => (t._1, v :: t._2))
          .value)

    /**
     * Splits a list of WDJ[A] into a tuple t of
     * (List[Lf], List[Lf], List[A]) where
     * t._1 are logged() results for Left values,
     * t._2 are logged() results for Right values, and
     * t._3 are Right values
     *
     * wr will be appended to Right logged() results if given
     */
    def tuple3(wr: Wr = wrM.zero):
    (List[Lf], List[Lf], List[A]) =
      lle.foldRight((List[Lf](), List[Lf](), List[A]()))((a, t) =>
        a.fold(_ => ((a.logged() :: t._1), t._2, t._3),
          s => (t._1, a.logged(wr) :: t._2, s :: t._3)).value)

    /**
     * Reduces a list of WDJ[A] to one WDJ[A] using
     * provided fn. Halts on first Left, but merges in
     * the logged() data from the entire list, ensuring that
     * log data from all elements is included in the final result.
     * In case of Left, Lf from the first encountered Left is appended
     * last.
     *
     * Result is Optional and will be None if the list is empty.
     */
    def loggedReduce(fn: (A, A) => A): Option[WDJ[A]] =
      lle.nonEmpty.fold(
        Some(lle.find(_.isLeft.run._2)
          .map(x => lfSg.append(x.logged(),
                        lle.filterNot(_ == x).map(_.logged())
                              .reduceRight(_|+|_))
                .leftWdj[Wr, A])
          .getOrElse(lle.reduce((x, y) => for {
                        z <- x
                        a <- y
                      } yield fn(z, a)))),
        None)

    /**
     * Perform a loggedReduce on this List, returning a Left
     * of the given Lf if the List is empty.
     */
    def loggedReduce(lm: Lf)(fn: (A, A) => A): WDJ[A] =
      loggedReduce(fn).getOrElse(lm.leftWdj[Wr, A])
  }
}
