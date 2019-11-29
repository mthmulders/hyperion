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
class RingBuffer[T](limit: Int) extends mutable.AbstractSeq[T] {
  val x = List()
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

  override def length: Int = items.count(_.isDefined)

  def +=(elem: T): RingBuffer.this.type = {
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
  override def iterator: scala.Iterator[T] = {
    new RingBufferIterator[T](this, monitor)
  }
}

object RingBuffer {
  def apply[T: ClassTag](size: Int): RingBuffer[T] = {
    new RingBuffer[T](size)
  }
}

class RingBufferIterator[T](src: RingBuffer[T], monitor: ReentrantReadWriteLock) extends Iterator[T] {
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
