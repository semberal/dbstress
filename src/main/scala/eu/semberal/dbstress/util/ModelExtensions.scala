package eu.semberal.dbstress.util

import breeze.linalg.DenseVector
import play.api.libs.json.{JsNull, JsNumber, JsValue}

object ModelExtensions {

  implicit class StatisticalExtensions(l: List[Double]) {

    private lazy val v = DenseVector(l: _*) // todo not elegant, take a look at it

    def median: Option[Double] = withNonEmptyList(breeze.stats.median apply _)

    def mean: Option[Double] = withNonEmptyList(breeze.stats.mean apply _)

    def variance: Option[Double] = withNonEmptyList(breeze.stats.variance apply _)

    def stddev: Option[Double] = withNonEmptyList(breeze.stats.stddev apply _)

    def minimum: Option[Double] = withNonEmptyList(breeze.linalg.min apply _)

    def maximum: Option[Double] = withNonEmptyList(breeze.linalg.max apply _)

    private def withNonEmptyList(f: DenseVector[Double] => Double) = if (l.isEmpty) None else Some(f(v))
  }

  implicit class StringifiedOption[T](o: Option[T]) {
    def getOrMissingString: String = o.map(_.toString).getOrElse("-")
  }

  implicit class NullableJsonValue(value: Option[Double]) {
    def getJsNumber: JsValue = value.map(JsNumber(_)).getOrElse(JsNull)
  }

}
