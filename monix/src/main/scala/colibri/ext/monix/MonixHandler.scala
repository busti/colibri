package colibri.ext.monix

import _root_.monix.execution.{Ack, Scheduler, Cancelable}
import _root_.monix.reactive.{Observable, Observer}
import _root_.monix.reactive.observers.Subscriber
import _root_.monix.reactive.subjects.{ReplaySubject, BehaviorSubject, PublishSubject}

import scala.concurrent.Future

object MonixSubject {
  def replay[T]: ReplaySubject[T] = ReplaySubject.createLimited(1)
  def behavior[T](seed:T): BehaviorSubject[T] = BehaviorSubject[T](seed)
  def publish[T]: PublishSubject[T] = PublishSubject[T]
}

object MonixProSubject {
  def replay[I,O](f: I => O): MonixProSubject[I,O] = MonixSubject.replay[I].mapObservable[O](f)
  def behavior[I,O](seed: I)(f: I => O): MonixProSubject[I,O] = MonixSubject.behavior[I](seed).mapObservable[O](f)
  def publish[I,O](f: I => O): MonixProSubject[I,O] = MonixSubject.publish[I].mapObservable[O](f)

  def apply[I,O](observer: Observer[I], observable: Observable[O]): MonixProSubject[I,O] = new Observable[O] with Observer[I] {
    override def onNext(elem: I): Future[Ack] = observer.onNext(elem)
    override def onError(ex: Throwable): Unit = observer.onError(ex)
    override def onComplete(): Unit = observer.onComplete()
    override def unsafeSubscribeFn(subscriber: Subscriber[O]): Cancelable = observable.unsafeSubscribeFn(subscriber)
  }
}
