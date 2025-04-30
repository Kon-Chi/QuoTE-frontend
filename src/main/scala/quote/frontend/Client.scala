package quote.frontend

import quote.ot.*
import quote.ot.OperationalTransformation.transform

sealed trait ClientState

case object ClientSynchronized extends ClientState
case class ClientWaiting(ops: List[Operation]) extends ClientState
case class ClientWaitingWithBuffer(
    waitingOp: List[Operation],
    bufferOp: List[Operation]
) extends ClientState

object ClientState {
  def initial: ClientState = ClientSynchronized
}

object Client {
  def applyClient(state: ClientState, ops: List[Operation]): (Boolean, ClientState) =
    state match {
      case ClientSynchronized =>
        (true, ClientWaiting(ops))

      case ClientWaiting(w) =>
        (false, ClientWaitingWithBuffer(w, ops))

      case ClientWaitingWithBuffer(w, b) =>
        (false, ClientWaitingWithBuffer(w, ops.reverse ::: b))
    }

  def applyServer(
      state: ClientState,
      serverOps: List[Operation]
  ): (List[Operation], ClientState) =
    state match {
      case ClientSynchronized =>
        (serverOps, ClientSynchronized)

      case ClientWaiting(w) =>
        val (w1, serverOps1) = transform(w, serverOps)
        (serverOps1, ClientWaiting(w1))

      case ClientWaitingWithBuffer(w, b) =>
        val (w1, serverOps1) = transform(w, serverOps)
        val (b1, serverOps2) = transform(b, serverOps1)
        (serverOps2, ClientWaitingWithBuffer(w1, b1))
    }

  def serverAck(state: ClientState): (Option[List[Operation]], ClientState) =
    state match {
      case ClientSynchronized =>
        (None, ClientSynchronized)

      case ClientWaiting(_) =>
        (None, ClientSynchronized)

      case ClientWaitingWithBuffer(_, b) =>
        (Some(b.reverse), ClientWaiting(b))
    }
}
