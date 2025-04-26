import scala.util.{Either, Left, Right}

sealed trait ClientState[Op] {
  def isSynchronized: Boolean = this match {
    case ClientSynchronized => true
    case _ => false
  }
}

case object ClientSynchronized extends ClientState[Nothing]
case class ClientWaiting[Op](op: Op) extends ClientState[Op]
case class ClientWaitingWithBuffer[Op](waitingOp: Op, bufferOp: Op) extends ClientState[Op]

object ClientState {
  def initial[Op]: ClientState[Op] = ClientSynchronized
}

trait OTComposableOperation[Op] {
  def compose(a: Op, b: Op): Either[String, Op]
  def transform(a: Op, b: Op): Either[String, (Op, Op)]
}

class Client[Op: OTComposableOperation] {
  private val ot = implicitly[OTComposableOperation[Op]]
  
  def applyClient(state: ClientState[Op], op: Op): Either[String, (Boolean, ClientState[Op])] = {
    state match {
      case ClientSynchronized => 
        Right((true, ClientWaiting(op)))
        
      case ClientWaiting(w) => 
        Right((false, ClientWaitingWithBuffer(w, op)))
        
      case ClientWaitingWithBuffer(w, b) =>
        ot.compose(b, op) match {
          case Left(err) => Left(s"operations couldn't be composed: $err")
          case Right(b1) => Right((false, ClientWaitingWithBuffer(w, b1)))
        }
    }
  }
  
  def applyServer(state: ClientState[Op], serverOp: Op): Either[String, (Op, ClientState[Op])] = {
    state match {
      case ClientSynchronized => 
        Right((serverOp, ClientSynchronized))
        
      case ClientWaiting(w) =>
        ot.transform(w, serverOp) match {
          case Left(err) => Left(s"transform failed: $err")
          case Right((w1, serverOp1)) => Right((serverOp1, ClientWaiting(w1)))
        }
        
      case ClientWaitingWithBuffer(w, b) =>
        for {
          (w1, serverOp1) <- ot.transform(w, serverOp).left.map(err => s"transform failed: $err")
          (b1, serverOp2) <- ot.transform(b, serverOp1).left.map(err => s"transform failed: $err")
        } yield (serverOp2, ClientWaitingWithBuffer(w1, b1))
    }
  }
  
  def serverAck(state: ClientState[Op]): Option[(Option[Op], ClientState[Op])] = {
    state match {
      case ClientSynchronized => 
        None
        
      case ClientWaiting(_) => 
        Some((None, ClientSynchronized))
        
      case ClientWaitingWithBuffer(_, b) => 
        Some((Some(b), ClientWaiting(b)))
    }
  }
}