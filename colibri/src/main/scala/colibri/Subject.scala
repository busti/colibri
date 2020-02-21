package colibri

import scala.scalajs.js

class ReplaySubject[I, O](convert: I => O) extends Observer[I] with Observable[O] {

  private var subscribers = new js.Array[Observer[O]]
  private var isRunning = false

  private var current: Option[O] = None

  @inline def now(): Option[O] = current

  def onNext(value: I): Unit = {
    isRunning = true
    val converted = convert(value)
    current = Some(converted)
    subscribers.foreach(_.onNext(converted))
    isRunning = false
  }

  def onError(error: Throwable): Unit = {
    isRunning = true
    subscribers.foreach(_.onError(error))
    isRunning = false
  }

  def subscribe[G[_] : Sink](sink: G[_ >: O]): Cancelable = {
    val observer = Observer.lift(sink)
    subscribers.push(observer)
    current.foreach(observer.onNext)
    Cancelable { () =>
      if (isRunning) subscribers = subscribers.filter(_ != observer)
      else JSArrayHelper.removeElement(subscribers)(observer)
    }
  }
}

class BehaviorSubject[I, O](private var current: O, convert: I => O) extends Observer[I] with Observable[O] {

  private var subscribers = new js.Array[Observer[O]]
  private var isRunning = false

  @inline def now(): O = current

  def onNext(value: I): Unit = {
    isRunning = true
    val converted = convert(value)
    current = converted
    subscribers.foreach(_.onNext(converted))
    isRunning = false
  }

  def onError(error: Throwable): Unit = {
    isRunning = true
    subscribers.foreach(_.onError(error))
    isRunning = false
  }

  def subscribe[G[_] : Sink](sink: G[_ >: O]): Cancelable = {
    val observer = Observer.lift(sink)
    subscribers.push(observer)
    observer.onNext(current)
    Cancelable { () =>
      if (isRunning) subscribers = subscribers.filter(_ != observer)
      else JSArrayHelper.removeElement(subscribers)(observer)
    }
  }
}

class PublishSubject[I, O](convert: I => O) extends Observer[I] with Observable[O] {

  private var subscribers = new js.Array[Observer[O]]
  private var isRunning = false

  def onNext(value: I): Unit = {
    isRunning = true
    val converted = convert(value)
    subscribers.foreach(_.onNext(converted))
    isRunning = false
  }

  def onError(error: Throwable): Unit = {
    isRunning = true
    subscribers.foreach(_.onError(error))
    isRunning = false
  }

  def subscribe[G[_] : Sink](sink: G[_ >: O]): Cancelable = {
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
  object replay {
    def apply[O]: ReplaySubject[O,O] = new ReplaySubject[O, O](identity)

    def map[I, O](convert: I => O): ReplaySubject[I, O] = new ReplaySubject[I, O](convert)
  }

  object behavior {
    def apply[O](seed: O): BehaviorSubject[O,O] = new BehaviorSubject[O, O](seed, identity)

    def map[I, O](seed: I)(convert: I => O): BehaviorSubject[I, O] = new BehaviorSubject[I, O](convert(seed), convert)
  }

  object publish {
    def apply[O]: PublishSubject[O,O] = new PublishSubject[O, O](identity)

    def map[I, O](convert: I => O): PublishSubject[I, O] = new PublishSubject[I, O](convert)
  }

  @inline def from[SI[_] : Sink, SO[_] : Source, I, O](sink: SI[I], source: SO[O]): Subject[I, O] = new CombinationSubject[SI, SO, I, O](sink, source)

  @inline implicit class Operations[I,O](val handler: Subject[I,O]) extends AnyVal {
    @inline def transformSource[S[_] : Source, O2](g: Observable[O] => S[O2]): Subject[I, O2] = from[Observer, S, I, O2](handler, g(handler))
    @inline def transformSink[G[_] : Sink, I2](f: Observer[I] => G[I2]): Subject[I2, O] = from[G, Observable, I2, O](f(handler), handler)
    @inline def transformSubject[G[_] : Sink, S[_] : Source, I2, O2](f: Observer[I] => G[I2])(g: Observable[O] => S[O2]): Subject[I2, O2] = from(f(handler), g(handler))
  }

  object createSubject extends CreateSubject[Lambda[X => Subject[X,X]]] {
    @inline def publish[A]: Subject[A,A] = Subject.publish[A]
    @inline def replay[A]: Subject[A,A] = Subject.replay[A]
    @inline def behavior[A](seed: A): Subject[A,A] = Subject.behavior[A](seed)
  }
  object createProSubject extends CreateProSubject[Subject] {
    @inline def publish[I,O](f: I => O): Subject[I,O] = Subject.publish.map(f)
    @inline def replay[I,O](f: I => O): Subject[I,O] = Subject.replay.map(f)
    @inline def behavior[I,O](seed: I)(f: I => O): Subject[I,O] = Subject.behavior.map(seed)(f)
    @inline def from[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): Subject[I, O] = Subject.from(sink, source)
  }
}
