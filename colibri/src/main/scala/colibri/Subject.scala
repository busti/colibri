package colibri

import scala.scalajs.js
import colibri.helpers._

class ReplaySubject[A] extends Observer[A] with Observable.MaybeValue[A] {

  private val state = new PublishSubject[A]

  private var current: Option[A] = None

  @inline def now(): Option[A] = current

  def onNext(value: A): Unit = {
    current = Some(value)
    state.onNext(value)
  }

  def onError(error: Throwable): Unit = state.onError(error)

  def subscribe[G[_] : Sink](sink: G[_ >: A]): Cancelable = {
    val cancelable = state.subscribe(sink)
    current.foreach(Sink[G].onNext(sink))
    cancelable
  }
}

class BehaviorSubject[A](private var current: A) extends Observer[A] with Observable.Value[A] {

  private val state = new PublishSubject[A]

  @inline def now(): A = current

  def onNext(value: A): Unit = {
    current = value
    state.onNext(value)
  }

  def onError(error: Throwable): Unit = state.onError(error)

  def subscribe[G[_] : Sink](sink: G[_ >: A]): Cancelable = {
    val cancelable = state.subscribe(sink)
    Sink[G].onNext(sink)(current)
    cancelable
  }
}

class PublishSubject[A] extends Observer[A] with Observable[A] {

  private var subscribers = new js.Array[Observer[A]]
  private var isRunning = false

  def onNext(value: A): Unit = {
    isRunning = true
    subscribers.foreach(_.onNext(value))
    isRunning = false
  }

  def onError(error: Throwable): Unit = {
    isRunning = true
    subscribers.foreach(_.onError(error))
    isRunning = false
  }

  def subscribe[G[_] : Sink](sink: G[_ >: A]): Cancelable = {
    val observer = Observer.lift(sink)
    subscribers.push(observer)
    Cancelable { () =>
      if (isRunning) subscribers = subscribers.filter(_ != observer)
      else JSArrayHelper.removeElement(subscribers)(observer)
    }
  }
}

object Subject {
  type Value[A] = Observer[A] with Observable.Value[A]
  type MaybeValue[A] = Observer[A] with Observable.MaybeValue[A]

  def replay[O]: ReplaySubject[O] = new ReplaySubject[O]

  def behavior[O](seed: O): BehaviorSubject[O] = new BehaviorSubject[O](seed)

  def publish[O]: PublishSubject[O] = new PublishSubject[O]

  @inline implicit class ValueOperations[A](val handler: Subject.Value[A]) extends AnyVal {
    def lens[B](read: A => B)(write: (A, B) => A): Subject.Value[B] = new Observer[B] with Observable.Value[B] {
      @inline def now() = read(handler.now())
      @inline def onNext(value: B): Unit = handler.onNext(write(handler.now(), value))
      @inline def onError(error: Throwable): Unit = handler.onError(error)
      @inline def subscribe[G[_] : Sink](sink: G[_ >: B]): Cancelable = handler.map(read).subscribe(sink)
    }
  }

  @inline implicit class MaybeValueOperations[A](val handler: Subject.MaybeValue[A]) extends AnyVal {
    def lens[B](seed: => A)(read: A => B)(write: (A, B) => A): Subject.MaybeValue[B] = new Observer[B] with Observable.MaybeValue[B] {
      @inline def now() = handler.now().map(read)
      @inline def onNext(value: B): Unit = handler.onNext(write(handler.now().getOrElse(seed), value))
      @inline def onError(error: Throwable): Unit = handler.onError(error)
      @inline def subscribe[G[_] : Sink](sink: G[_ >: B]): Cancelable = handler.map(read).subscribe(sink)
    }
  }
}

object ProSubject {
  type Value[-I,+O] = Observer[I] with Observable.Value[O]
  type MaybeValue[-I,+O] = Observer[I] with Observable.MaybeValue[O]

  def from[SI[_] : Sink, SO[_] : Source, I, O](sink: SI[I], source: SO[O]): ProSubject[I, O] = new Observer[I] with Observable[O] {
    @inline def onNext(value: I): Unit = Sink[SI].onNext(sink)(value)
    @inline def onError(error: Throwable): Unit = Sink[SI].onError(sink)(error)
    @inline def subscribe[G[_] : Sink](sink: G[_ >: O]): Cancelable = Source[SO].subscribe(source)(sink)
  }

  @inline implicit class Operations[I,O](val handler: ProSubject[I,O]) extends AnyVal {
    @inline def transformSubjectSource[S[_] : Source, O2](g: Observable[O] => S[O2]): ProSubject[I, O2] = from[Observer, S, I, O2](handler, g(handler))
    @inline def transformSubjectSink[G[_] : Sink, I2](f: Observer[I] => G[I2]): ProSubject[I2, O] = from[G, Observable, I2, O](f(handler), handler)
    @inline def transformSubject[G[_] : Sink, S[_] : Source, I2, O2](f: Observer[I] => G[I2])(g: Observable[O] => S[O2]): ProSubject[I2, O2] = from(f(handler), g(handler))
  }
}
