package eu.semberal.dbstress.util

import breeze.linalg.DenseVector
import play.api.libs.json.{JsNull, JsNumber, JsValue}

object ModelExtensions {

  implicit class StatisticalExtensions(l: List[Long]) {

    private lazy val v = DenseVector(l: _*)

    def median: Option[Long] = v.execOpt(x => breeze.stats.median(x))

    def mean: Option[Double] = v.map(_.toDouble).execOpt(x => breeze.stats.mean(x))

    def variance: Option[Double] = v.map(_.toDouble).execOpt(x => breeze.stats.variance(x))

    def stddev: Option[Double] = v.map(_.toDouble).execOpt(x => breeze.stats.stddev(x))

    def minimum: Option[Long] = v.execOpt(x => breeze.linalg.min(x))

    def maximum: Option[Long] = v.execOpt(x => breeze.linalg.max(x))
  }

  implicit class DenseVectorToOpt[A](v: DenseVector[A]) {
    def execOpt[B](f: DenseVector[A] => B): Option[B] = if (v.length == 0) None else Some(f(v))
  }

  implicit class StringifiedOption[T](o: Option[T]) {
    def getOrMissingString: String = o.map(_.toString).getOrElse("-")
  }

  implicit class NullableJsonValue[T <% BigDecimal](value: Option[T]) {
    def getJsNumber: JsValue = value.map(JsNumber(_)).getOrElse(JsNull)
  }

}
