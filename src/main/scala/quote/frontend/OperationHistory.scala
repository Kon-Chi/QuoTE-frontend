package quote.frontend

import quote.ot.*

type Storage = List[(Operation, Unit => Operation)]

object OperationHistory:
  private var doneOperations: Storage = Nil
  private var doneUndoOperations: Storage = Nil

  private def storeOperation(op: Operation, inverseOp: Unit => Operation, storage: Storage): Storage =
    (op, inverseOp) :: storage

  def putOp(op: Operation): Unit =
    doneOperations = storeOperation(op, _ => getInverse(op), doneOperations)

  def putUndoOp(op: Operation): Unit =
    doneUndoOperations = storeOperation(op, _ => getInverse(op), doneUndoOperations)

  def revertOp: Operation =
    val (_, undo) = doneOperations.head
    doneOperations = doneOperations.drop(1)
    undo(())

  def revertUndoOp: Operation =
    val (_, redu) = doneUndoOperations.head
    doneUndoOperations = doneUndoOperations.drop(1)
    redu(())

def getInverse(op: Operation): Operation = op match
  case Insert(ind, str) => Delete(ind, str)
  case Delete(ind, str) => Insert(ind, str)
