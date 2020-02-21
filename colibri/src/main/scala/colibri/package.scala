package object colibri {
  type Subject[-I,+O] = Observer[I] with Observable[O]
}
