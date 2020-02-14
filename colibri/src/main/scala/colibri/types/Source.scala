package colibri

trait Source[-F[_]] {
  def subscribe[G[_] : Sink, A, S: Subscription](source: F[A])(sink: G[_ >: A]): S
}
object Source {
  @inline def apply[F[_]](implicit source: Source[F]): Source[F] = source
}

trait LiftSource[+F[_]] {
  def lift[G[_] : Source, A](source: G[A]): F[A]
}
object LiftSource {
  @inline def apply[F[_]](implicit source: LiftSource[F]): LiftSource[F] = source
}
