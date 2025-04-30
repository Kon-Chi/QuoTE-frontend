package quote.frontend

import quote.ot.*

type Storage = List[Unit => Operation]

object OperationHistory:
  private var doneOperations: Storage = Nil
  private var doneUndoOperations: Storage = Nil

  private def storeOperation(inverseOp: Unit => Operation, storage: Storage): Storage =
    inverseOp :: storage

  def putOp(op: Operation): Unit =
    doneOperations = storeOperation(_ => getInverse(op), doneOperations)

  def putUndoOp(op: Operation): Unit =
    doneUndoOperations = storeOperation(_ => getInverse(op), doneUndoOperations)

  def revertOp: Operation =
    val undo = doneOperations.head
    doneOperations = doneOperations.drop(1)
    undo(())

  def revertUndoOp: Operation =
    val redu = doneUndoOperations.head
    doneUndoOperations = doneUndoOperations.drop(1)
    redu(())

def getInverse(op: Operation): Operation = op match
  case Insert(ind, str) => Delete(ind, str)
  case Delete(ind, str) => Insert(ind, str)
