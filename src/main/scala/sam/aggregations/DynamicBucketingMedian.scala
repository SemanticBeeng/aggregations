package sam.aggregations

import scala.collection.mutable

object DynamicBucketingMedian {
  def mergeSmallestConsequtive(m: mutable.Map[(Long, Long), Long]): Unit = ???
}

import DynamicBucketingMedian._

// Not thread safe
class DynamicBucketingMedian(sizeLimit: Int) extends Median[DynamicBucketingMedian] {
  val exactMedian = new ExactMedian()

  def size: Int = m.size

  def getMap: Map[(Long, Long), Long] = m.toMap

  private val m: mutable.Map[(Long, Long), Long] = mutable.Map.empty

  def update(e: Long): Unit = {
    if (exactMedian.getElems.size == sizeLimit) {
      exactMedian.getElems.foreach(old => m += ((old, old) -> 1))

      m += ((e, e) -> 1)

      mergeSmallestConsequtive(m)
    } else {
      exactMedian.update(e)
    }
  }

  def result: Double = if (m.isEmpty) exactMedian.result else {
    val keys = m.keySet.toList.sorted.map(_._1)
    keys(keys.size / 2)
  }

  def update(m: DynamicBucketingMedian): Unit = exactMedian.update(m.exactMedian)
}




//object DynamicBucketingMedian {
//  def addThenMerge(maxMapSize: Int, balanceRatio: Double, m: mutable.Map[(Long, Long), Long], e: Int): Unit = {
//
//
//
//  }
//
//  // Simple algo that will merge the two adjacent buckets that will result in the smallest increase in range
//  // I.e. merge the closest buckets
//  def mergeClosestBuckets(m: mutable.Map[(Long, Long), Long]): Unit = ???
//
//}

//case class DynamicBucketingMedian(maxMapSize: Int, balanceRatio: Double = 1.0) extends Median {
//  // first implementation will use a mutable map, should be replaced with custom Array based implementation
//
//  private val m: mutable.Map[(Long, Long), Long] = mutable.Map.empty
//
//  def update(e: Long): Unit = ???
//  def result: Double = ???
//  def update(m: Median): Unit = ???
//}
