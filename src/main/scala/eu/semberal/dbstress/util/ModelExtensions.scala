package eu.semberal.dbstress.util

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.util.Precision

object ModelExtensions {

  implicit class StatisticalExtensions(l: List[Long]) {

    private lazy val ds = new DescriptiveStatistics(l.map(_.toDouble).toArray)

    private def doCalculate(
        f: DescriptiveStatistics => Double
    ): Option[Double] = if (l.isEmpty) None else Some(f(ds))

    def median: Option[Double] = doCalculate(_.getPercentile(50))

    def p90: Option[Double] = doCalculate(_.getPercentile(90))

    def p99: Option[Double] = doCalculate(_.getPercentile(99))

    def mean: Option[Double] = doCalculate(_.getMean)

    def stddev: Option[Double] = doCalculate(_.getStandardDeviation)

    def minimum: Option[Long] = if (l.isEmpty) None else Some(l.min)

    def maximum: Option[Long] = if (l.isEmpty) None else Some(l.max)
  }

  implicit class DoubleOpt[T](o: Option[T]) {
    def getOrMissingString: String = o
      .map {
        case x: Double => Precision.round(x, 2).toString
        case x         => x.toString
      }
      .getOrElse("-")
  }

}
