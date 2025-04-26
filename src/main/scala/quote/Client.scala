import scala.util.{Either, Left, Right}

sealed trait ClientState[+Op]
case object ClientSynchronized extends ClientState[Nothing]
case class ClientWaiting[Op](op: Op) extends ClientState[Op]
case class ClientWaitingWithBuffer[Op](waitingOp: Op, bufferOp: Op) extends ClientState[Op]

object ClientState {
  def initial[Op]: ClientState[Op] = ClientSynchronized
}

trait OTComposableOperation[Op] {
  def compose(a: Op, b: Op): Op
  def transform(a: Op, b: Op): (Op, Op)
}

class Client[Op](implicit ot: OTComposableOperation[Op]) {
  
  def applyClient(state: ClientState[Op], op: Op): (Boolean, ClientState[Op]) = {
    state match {
      case ClientSynchronized => 
        (true, ClientWaiting(op))
        
      case ClientWaiting(w) => 
        (false, ClientWaitingWithBuffer(w, op))
        
      case ClientWaitingWithBuffer(w, b) =>
        val b1 = ot.compose(b, op)
        (false, ClientWaitingWithBuffer(w, b1))
    }
  }
  
  def applyServer(state: ClientState[Op], serverOp: Op): (Op, ClientState[Op]) = {
    state match {
      case ClientSynchronized => 
        (serverOp, ClientSynchronized)
        
      case ClientWaiting(w) =>
        val (w1, serverOp1) = ot.transform(w, serverOp)
        (serverOp1, ClientWaiting(w1))
        
      case ClientWaitingWithBuffer(w, b) =>
        val (w1, serverOp1) = ot.transform(w, serverOp)
        val (b1, serverOp2) = ot.transform(b, serverOp1)
        (serverOp2, ClientWaitingWithBuffer(w1, b1))
    }
  }
  
  def serverAck(state: ClientState[Op]): (Option[Op], ClientState[Op]) = {
    state match {
      case ClientSynchronized => 
        (None, ClientSynchronized)
        
      case ClientWaiting(_) => 
        (None, ClientSynchronized)
        
      case ClientWaitingWithBuffer(_, b) => 
        (Some(b), ClientWaiting(b))
    }
  }
}