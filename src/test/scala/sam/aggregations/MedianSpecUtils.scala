package sam.aggregations

import org.scalacheck.{Prop, Arbitrary, Gen}
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.specs2.mutable.Specification

import scala.util.{Random, Success, Try}

object MedianSpecUtils {
  // Move to unit tests and wrap other code
  def normalDistribution[M <: Aggregator[Double, Long, M]](median: M, n: Int, max: Int): Double =
    ErrorEstimator.relativeError(ErrorEstimator.cappedNormal(max).sample(n), median)

  val rand = new Random()

  def uniformDistribution[M <: Aggregator[Double, Long, M]](median: M, n: Int, max: Int): Double =
    ErrorEstimator.relativeError((1 to n).map(_ => rand.nextInt(max).toLong), median)
}

import MedianSpecUtils._

class MedianSpecUtils extends Specification with ScalaCheck {

  implicit def toProp(m: MatchResult[Any]): Prop = resultProp(m)

  def basicMedianSpecs[T <: Aggregator[Double, Long, T]](fac: () => T, desc: String = "ExactMedian"): Unit =
    "Median aggregator " + desc should {
      "Throw exception when called with no update ever being called" in {
        val median = fac()
        Try(median.result) match {
          case _: Success[_] => failure("Did not throw exception but should have done")
          case t if t.failed.get.isInstanceOf[IllegalArgumentException] => success
          case t if t.failed.get.isInstanceOf[IllegalArgumentException] =>
            failure("Wrong type of exception: " + t.failed.get)
        }
      }

      "Return 6 when call update with just 6" in {
        val median = fac()
        median.update(6L)
        median.result must_=== 6.0
      }

      "Return 88 when call update with just 88" in {
        val median = fac()
        median.update(88L)
        median.result must_=== 88.0
      }

      "Return single element when call update with just a single element" ! check((elem: Long) => {
        val median = fac()
        median.update(elem)
        median.result must_=== elem.toDouble
      })

      "Return 4 when call update with just 4 and 4" in {
        val median = fac()
        median.update(4L)
        median.update(4L)
        median.result must_=== 4.0
      }

      "Return 77 when call update with just 77 and 77" in {
        val median = fac()
        median.update(77L)
        median.update(77L)
        median.result must_=== 77.0
      }

      "Return 1.5 when call update with just 1 and 2" in {
        val median = fac()
        median.update(1L)
        median.update(2L)
        median.result must_=== 1.5
      }

      "Return 2 when call update with just 1 and 2 and 3" in {
        val median = fac()
        median.update(1L)
        median.update(2L)
        median.update(3L)
        median.result must_=== 2.0
      }

      "Return 2 when call update with just 3 and 1 and 2" in {
        val median = fac()
        median.update(3L)
        median.update(1L)
        median.update(2L)
        median.result must_=== 2.0
      }

      "Return 4.5 when call update with just 3 and 6 and 4 and 5" in {
        val median = fac()
        median.update(3L)
        median.update(6L)
        median.update(4L)
        median.update(5L)
        median.result must_=== 4.5
      }

      "Return 55 when call update with just 55 and 456 and 4 and 5 and 999" in {
        val median = fac()
        median.update(55L)
        median.update(456L)
        median.update(4L)
        median.update(5L)
        median.update(999L)
        median.result must_=== 55.0
      }

      "Return 60 when call update with just 55 and 456 and 4 and 5 and 999 and 65" in {
        val median = fac()
        median.update(55L)
        median.update(456L)
        median.update(4L)
        median.update(5L)
        median.update(999L)
        median.update(65L)
        median.result must_=== 60.0
      }

    }

  def sufficientMemoryProperties[T <: Aggregator[Double, Long, T]](memCappedFac: Int => T): Unit = {
    "median with sufficient memory 1" should {
      implicit val arbitraryParams: Arbitrary[(Int, Int, Int)] = Arbitrary(
        for {
          limit <- Gen.frequency((1, 10), (1, 20), (1, 50))
          n <- Gen.choose(5, limit)
          max <- Gen.choose(5, 500)
        } yield (limit, n, max)
      )

      "Give correct answer for Normal dist when number of examples is less than limit" ! check(
        (params: (Int, Int, Int)) => (params.productIterator.forall(_.asInstanceOf[Int] > 0) &&
          params._2 <= params._1) ==> (params match {
          case (limit, n, max) =>
            val median = memCappedFac(limit)
            normalDistribution(median, n, max) must_=== 0.0
        }))
      
      "return 60 when we update via another median with just 55 and 456 and 4 and 5 and 999 and 65" in {
        val median = memCappedFac(10)
        median.update(55L)
        median.update(456L)
        median.update(4L)

        val median2 = memCappedFac(10)
        median2.update(5L)
        median2.update(999L)
        median2.update(65L)

        median.update(median2)

        median.result must_=== 60.0
      }
    }
    
    "median with sufficient memory 2" should {
      val bigMemCap = 500
      val longGen = Gen.choose(Long.MinValue / 2, Long.MaxValue / 2)
      implicit val arbitraryListLongUpTo50: Arbitrary[List[Long]] = Arbitrary(Gen.frequency(
        (1, Gen.listOfN(1, longGen)),
        (1, Gen.listOfN(2, longGen)),
        (1, Gen.listOfN(3, longGen)),
        (1, Gen.listOfN(5, longGen)),
        (1, Gen.listOfN(10, longGen)),
        (1, Gen.listOfN(25, longGen)),
        (1, Gen.listOfN(50, longGen))
      ))

      "2 medians produce same results as a single median" ! check((l1: List[Long], l2: List[Long]) => {
        val median = memCappedFac(bigMemCap)

        Random.shuffle(l1 ++ l2).foreach(median.update)

        val median1 = memCappedFac(bigMemCap)
        val median2 = memCappedFac(bigMemCap)

        l1.foreach(median1.update)
        l2.foreach(median2.update)

        median1.update(median2)

        median1.result must_=== median.result
      })

      "5 medians produce same results as a single median" ! check(
        (l1: List[Long], l2: List[Long], l3: List[Long], l4: List[Long], l5: List[Long]) => {
          val median = memCappedFac(bigMemCap)

          Random.shuffle(l1 ++ l2 ++ l3 ++ l4 ++ l5).foreach(median.update)

          val median1 = memCappedFac(bigMemCap)
          val median2 = memCappedFac(bigMemCap)
          val median3 = memCappedFac(bigMemCap)
          val median4 = memCappedFac(bigMemCap)
          val median5 = memCappedFac(bigMemCap)

          l1.foreach(median1.update)
          l2.foreach(median2.update)
          l3.foreach(median3.update)
          l4.foreach(median4.update)
          l5.foreach(median5.update)

          median1.update(median2)
          median1.update(median3)
          median1.update(median4)
          median1.update(median5)

          median1.result must_=== median.result
        })
    }
  }

  def medianIsCommutative[T <: Aggregator[Double, Long, T]](memCappedFac: Int => T): Unit =
    "Median" should {
      val longGen = Gen.choose(Long.MinValue / 2, Long.MaxValue / 2)
      implicit val arbitraryListLongUpTo50: Arbitrary[List[Long]] = Arbitrary(Gen.frequency(
        (1, Gen.listOfN(1, longGen)),
        (1, Gen.listOfN(2, longGen)),
        (1, Gen.listOfN(3, longGen)),
        (1, Gen.listOfN(5, longGen)),
        (1, Gen.listOfN(10, longGen)),
        (1, Gen.listOfN(25, longGen)),
        (1, Gen.listOfN(50, longGen))
      ))

      "Order not important even with small cap (i.e. is commutative)" ! check((l1: List[Long], l2: List[Long]) =>
        (l1.nonEmpty && l2.nonEmpty) ==> {
          val smallCap = 10

          val median1 = memCappedFac(smallCap)
          val median1Copy = memCappedFac(smallCap)
          val median2 = memCappedFac(smallCap)

          l1.foreach(median1.update)
          l1.foreach(median1Copy.update)
          l2.foreach(median2.update)

          median1.update(median2)
          median2.update(median1Copy)

          median1.result must_=== median2.result
        })
    }

}
