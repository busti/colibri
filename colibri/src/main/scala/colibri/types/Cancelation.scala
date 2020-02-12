package colibri

trait Cancelation[T] {
  def cancel(subscription: T): Unit
}
object Cancelation {
  @inline def apply[T](implicit cancelation: Cancelation[T]): Cancelation[T] = cancelation
}

trait Subscription[+T] {
  def apply[C: Cancelation](subscription: () => C): T
}
object Subscription {
  @inline def apply[T](implicit subscription: Subscription[T]): Subscription[T] = subscription
}
