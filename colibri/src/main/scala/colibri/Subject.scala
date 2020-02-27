package colibri

import scala.scalajs.js

class ReplaySubject[A] extends Observer[A] with Observable.MaybeValue[A] {

  private var subscribers = new js.Array[Observer[A]]
  private var isRunning = false

  private var current: Option[A] = None

  @inline def now(): Option[A] = current

  def onNext(value: A): Unit = {
    isRunning = true
    current = Some(value)
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
    current.foreach(observer.onNext)
    Cancelable { () =>
      if (isRunning) subscribers = subscribers.filter(_ != observer)
      else JSArrayHelper.removeElement(subscribers)(observer)
    }
  }
}

class BehaviorSubject[A](private var current: A) extends Observer[A] with Observable.Value[A] {

  private var subscribers = new js.Array[Observer[A]]
  private var isRunning = false

  @inline def now(): A = current

  def onNext(value: A): Unit = {
    isRunning = true
    current = value
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
    observer.onNext(current)
    Cancelable { () =>
      if (isRunning) subscribers = subscribers.filter(_ != observer)
      else JSArrayHelper.removeElement(subscribers)(observer)
    }
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

@inline class CombinationSubject[SI[_] : Sink, SO[_] : Source, I, O](sink: SI[I], source: SO[O]) extends Observer[I] with Observable[O] {

  @inline def onNext(value: I): Unit = Sink[SI].onNext(sink)(value)

  @inline def onError(error: Throwable): Unit = Sink[SI].onError(sink)(error)

  @inline def subscribe[G[_] : Sink](sink: G[_ >: O]): Cancelable = Source[SO].subscribe(source)(sink)
}

object Subject {
  def replay[O]: ReplaySubject[O] = new ReplaySubject[O]

  def behavior[O](seed: O): BehaviorSubject[O] = new BehaviorSubject[O](seed)

  def publish[O]: PublishSubject[O] = new PublishSubject[O]

  @inline def from[SI[_] : Sink, SO[_] : Source, I, O](sink: SI[I], source: SO[O]): ProSubject[I, O] = new CombinationSubject[SI, SO, I, O](sink, source)

  @inline implicit class Operations[I,O](val handler: ProSubject[I,O]) extends AnyVal {
    @inline def transformSource[S[_] : Source, O2](g: Observable[O] => S[O2]): ProSubject[I, O2] = from[Observer, S, I, O2](handler, g(handler))
    @inline def transformSink[G[_] : Sink, I2](f: Observer[I] => G[I2]): ProSubject[I2, O] = from[G, Observable, I2, O](f(handler), handler)
    @inline def transformSubject[G[_] : Sink, S[_] : Source, I2, O2](f: Observer[I] => G[I2])(g: Observable[O] => S[O2]): ProSubject[I2, O2] = from(f(handler), g(handler))
  }

  object createSubject extends CreateSubject[Subject] {
    @inline def publish[A]: Subject[A] = Subject.publish[A]
    @inline def replay[A]: Subject[A] = Subject.replay[A]
    @inline def behavior[A](seed: A): Subject[A] = Subject.behavior[A](seed)
  }
  object createProSubject extends CreateProSubject[ProSubject] {
    @inline def from[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): ProSubject[I, O] = Subject.from(sink, source)
  }
}
