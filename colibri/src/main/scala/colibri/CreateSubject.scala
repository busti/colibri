package colibri

trait CreateSubject[+F[_]] {
  def publish[A]: F[A]
  def replay[A]: F[A]
  def behavior[A](seed: A): F[A]
}
object CreateSubject {
  @inline def apply[F[_]](implicit handler: CreateSubject[F]): CreateSubject[F] = handler
}

trait CreateProSubject[+F[_,_]] {
  def from[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): F[I, O]
}
object CreateProSubject {
  @inline def apply[F[_,_]](implicit handler: CreateProSubject[F]): CreateProSubject[F] = handler

  object subject extends CreateSubject[Subject] {
    @inline def publish[A]: Subject[A] = Subject.publish[A]
    @inline def replay[A]: Subject[A] = Subject.replay[A]
    @inline def behavior[A](seed: A): Subject[A] = Subject.behavior[A](seed)
  }
  object proSubject extends CreateProSubject[ProSubject] {
    @inline def from[SI[_] : Sink, SO[_] : Source, I,O](sink: SI[I], source: SO[O]): ProSubject[I, O] = Subject.from(sink, source)
  }
}
