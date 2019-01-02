package hyperion

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

import scala.collection._
import scala.reflect.ClassTag

/**
  * A collection that is limited in size and will purge the oldest items when the limit is exceeded.
  * This allows for efficient memory allocation and drops the need to re-allocate arrays.
  *
  * @param limit Maximum number of elements in this collection.
  */
class RingBuffer[T](limit: Int)(implicit m: ClassTag[T]) extends mutable.AbstractBuffer[T] with GenTraversable[T] {
  private[this] val items = Array.fill[Option[T]](limit)(None)
  private[this] val cursor = new AtomicInteger(0)
  private[this] val monitor = new ReentrantReadWriteLock()

  private[this] def positionInArray(desiredPosition: Int): Int = {
    if (length >= limit) (cursor.get() + desiredPosition) % limit else desiredPosition
  }

  override def apply(n: Int): T = {
    monitor.readLock().lock()
    try {
      items(positionInArray(n)) match {
        case Some(item) => item
        case None => throw new NoSuchElementException
      }
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

  override def clear(): Unit = {
    // Not needed, as this implementation will overwrite previous items either way.
  }

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
      items.update(cursor.get(), Some(elem))
      if (cursor.incrementAndGet() >= limit) cursor.set(0)
      this
    } finally {
      monitor.writeLock().unlock()
    }
  }

  /** @inheritdoc */
  override def insertAll(n: Int, elems: scala.Traversable[T]): Unit = {
    val elemsList = elems.seq.toList
    for (i <- 0 until elems.size) update(n + i, elemsList(i))
  }

  /** @inheritdoc */
  override def iterator: scala.Iterator[T] = {
    new RingBufferIterator[T](this, cursor.get(), monitor)
  }
}

object RingBuffer {
  def apply[T: ClassTag](size: Int): RingBuffer[T] = {
    new RingBuffer[T](size)
  }
}

class RingBufferIterator[T](src: RingBuffer[T], startPos: Int, monitor: ReentrantReadWriteLock) extends Iterator[T] {
  private[this] val cursor = new AtomicInteger(0)

  override def hasNext: Boolean = cursor.get() < src.length

  override def next(): T = {
    monitor.readLock().lock()
    try {
      src(cursor.getAndIncrement())
    } finally {
      monitor.readLock().unlock()
    }
  }
}