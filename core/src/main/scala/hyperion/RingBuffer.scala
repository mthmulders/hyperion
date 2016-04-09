package hyperion

import java.util.concurrent.locks.ReentrantReadWriteLock

import scala.annotation.migration
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.IndexedSeq
import scala.collection._
import scala.collection.parallel.{ParSeq, Combiner}
import scala.reflect.ClassTag

/**
  * A collection that is limited in size and will purge the oldest items when the limit is exceeded.
  * This allows for efficient memory allocation and drops the need to re-allocate arrays.
 *
  * @param limit Maximum number of elements in this collection.
  */
class RingBuffer[T](limit: Int)(implicit m: ClassTag[T]) extends mutable.AbstractBuffer[T] with GenTraversable[T] {
  private[this] val items = Array.fill[Option[T]](limit)(None)
  private[this] var cursor = 0
  private[this] val monitor = new ReentrantReadWriteLock()

  private[this] def positionInArray(desiredPosition: Int): Int = {
    (cursor + desiredPosition) % limit
  }

  override def apply(n: Int): T = {
    monitor.readLock().lock()
    try {
      items(positionInArray(n)).get
    } finally {
      monitor.readLock().unlock()
    }
  }

  override def update(n: Int, newelem: T): Unit = {
    monitor.writeLock().lock()
    try {
      items.update(positionInArray(n), Some(newelem))
    } finally {
      monitor.writeLock().unlock()
    }
  }

  override def clear(): Unit = {}

  override def length: Int = items.count(_.isDefined)

  /**
    * Removes the element at a given index from this buffer.
    * Maintains the contract, but does not actually remove the element.
    *
    * @param n the index which refers to the element to delete.
    * @return the previous element at index `n`.
    */
  override def remove(n: Int): T = apply(n)

  /** @inheritdoc */
  override def +=:(elem: T): RingBuffer.this.type = ???

  /** @inheritdoc */
  override def +=(elem: T): RingBuffer.this.type = {
    monitor.writeLock().lock()
    try {
      items.update(cursor, Some(elem))
      cursor += 1
      if (cursor >= limit) cursor = 0
      this
    } finally {
      monitor.writeLock().unlock()
    }
  }

  override def insertAll(n: Int, elems: scala.Traversable[T]): Unit = ???

  /** @inheritdoc */
  override def iterator: scala.Iterator[T] = {
    new RingBufferIterator[T](this, cursor, monitor)
  }
}

object RingBuffer {
  def apply[T: ClassTag](size: Int) = {
    new RingBuffer[T](size)
  }
}

class RingBufferIterator[T](src: RingBuffer[T], startPos: Int, monitor: ReentrantReadWriteLock) extends Iterator[T] {
  private[this] var cursor = 0

  override def hasNext: Boolean = cursor < src.length

  override def next(): T = {
    monitor.readLock().lock()
    try {
      val r = src(cursor)
      cursor += 1
      r
    } finally {
      monitor.readLock().unlock()
    }
  }
}