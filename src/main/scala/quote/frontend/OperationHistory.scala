package quote.frontend

import quote.ot.*

type Storage = List[(Operation, Unit => Operation)]

object OperationHistory:
  private var doneOperations: Storage = Nil
  private var doneUndoOperations: Storage = Nil

  private def storeOperation(op: Operation, inverseOp: Unit => Operation, storage: Storage): Storage =
    (op, inverseOp) :: storage

  def putInsertOp(op: Insert): Unit =
    doneOperations = storeOperation(op, _ => getInverseFromInsert(op), doneOperations)

  def putDeleteOp(op: Delete, context: String): Unit =
    doneOperations = storeOperation(op, _ => getInverseFromDelete(op, context), doneOperations)

  def putUndoInsertOp(op: Insert): Unit =
    doneUndoOperations = storeOperation(op, _ => getInverseFromInsert(op), doneUndoOperations)

  def putUndoDeleteOp(op: Delete, context: String): Unit =
    doneUndoOperations = storeOperation(op, _ => getInverseFromDelete(op, context), doneUndoOperations)

  def revertOp: Operation =
    val (_, undo) = doneOperations.head
    doneOperations = doneUndoOperations.drop(1)
    undo(()) match
      case undo @ Insert(_, _) => undo
      case undo @ Delete(_, _) => undo


  def revertUndoOp: Operation =
    val (_, redu) = doneUndoOperations.head
    doneUndoOperations = doneUndoOperations.drop(1)
    redu(()) match
      case undo @ Insert(_, _) => undo
      case undo @ Delete(_, _) => undo


def getInverseFromInsert(op: Insert): Delete =
  op match
    case Insert(ind, str) => Delete(ind, str.length)

def getInverseFromDelete(op: Delete, context: String): Insert =
  op match
    case Delete(ind, len) => Insert(ind, context.substring(ind, ind + len))
