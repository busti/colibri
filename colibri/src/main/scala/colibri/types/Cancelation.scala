package colibri

trait Cancelation[T] {
  def cancel(subscription: T): Unit
}
object Cancelation {
  @inline def apply[T](implicit cancelation: Cancelation[T]): Cancelation[T] = cancelation
}
