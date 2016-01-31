package com.localytics

import scala.language.implicitConversions
import scalaz.{\/, EitherT, Monoid, Semigroup, Writer}
import scalaz.syntax.writer._

/**
 * Trait providing a WDJ (Writer Disjunction) for a given
 * Writer type (Wr) and -\/ type (Lf)
 */
trait HasWDJ[Wr, Lf] {
  type Logger[A] = Writer[Wr, A]
  type V[A] = Logger[\/[Lf, A]]
  type WDJ[A] = EitherT[Logger, Lf, A]

  implicit val wrM: Monoid[Wr]
  implicit val lfSg: Semigroup[Lf]
  implicit def wrToLf: Wr => Lf
  implicit def vToWdj[A](v: V[A]): WDJ[A] =
    EitherT(v)
  implicit def djToWdj[A](dj: \/[Lf, A]): WDJ[A] =
    dj.set(wrM.zero)

}

object WDJ {

  /**
   * Construct a new HasWDJ for a given Wr and Lf, inferred from the provided
   * Wr => Lf. Requires a Monoid[Wr] and Semigroup[Lf]
   */
  def apply[Wr, Lf](fn: Wr => Lf)(implicit m: Monoid[Wr], sg: Semigroup[Lf]) =
    new HasWDJ[Wr, Lf] {
      implicit val wrM = m
      implicit val lfSg = sg
      implicit def wrToLf = fn
    }
}
