package com.micronautics.publish

import java.util.UUID

object Evaluation {
  def default: Evaluation[Unit, Unit] = new Evaluation[Unit, Unit](identity)
}

case class Evaluation[From, +To](evaluate: (From) => To)

object FSMLike {
  val uuidEmpty: UUID = new UUID(0L, 0L)

//  val endState: Evaluation[Unit, FSMLike[Nothing]] = new Evaluation(() => FSMEmpty)
//
//  lazy val stop: FSMEntry[Unit, Unit, Nothing] = FSMEntry(uuidEmpty, Evaluation.default, endState)

  def apply[From1, From2, To](
    action: Evaluation[From1, Unit],
    nextState: Evaluation[From2, FSMLike[To]]
  ): (UUID, FSMLike[To]) = {
    val uuid = UUID.randomUUID
    uuid -> FSMEntry(uuid, action, nextState)
  }
}

sealed trait FSMLike[+A]

case object FSMEmpty extends FSMLike[Nothing]

case class FSMEntry[From1, From2, +To](
  id: UUID,
  action: Evaluation[From1, Unit],
  nextState: Evaluation[From2, FSMLike[To]]
) extends FSMLike[To] {
  def transition(arg1: From1, arg2: From2): FSMLike[To] = {
    action.evaluate(arg1)
    nextState.evaluate(arg2)
  }

  override def toString: String = s"^$id^"
}

case class FiniteStateMachine(states: Map[UUID, FSMLike[_]]) {
  assert(states.nonEmpty, "FiniteStateMachines must have at least one state")

  var currentState: FSMLike[_] = states.toSeq.head._2

  def nextState[From1, From2](arg1: From1, arg2: From2): FSMLike[_] = currentState match {
    case FSMEmpty =>
      // todo stop the program, we are done
      FSMEmpty

    case fsmEntry @ FSMEntry(_, _, _) => fsmEntry.transition(arg1, arg2)
  }
}
