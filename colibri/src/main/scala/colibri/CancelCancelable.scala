package colibri

trait CancelCancelable[-T] {
  def cancel(cancelable: T): Unit
}
object CancelCancelable {
  @inline def apply[T](implicit cancel: CancelCancelable[T]): CancelCancelable[T] = cancel
}
