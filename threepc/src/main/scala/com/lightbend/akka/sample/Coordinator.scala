//#full-example
package com.lightbend.akka.sample

import akka.actor.{ActorRef, ActorSystem, FSM, Props}

import scala.concurrent.duration._


// received events
final case class Request(request: String)
final case class Response(request: String, response: String)
final case class ACK(request: String)

// sent events
final case class canCommit(request: String)
final case class Abort(request: String)
final case class preCommit(request: String)
final case class doCommit(request: String)

// states
sealed trait State
case object Idle extends State
case object Waiting extends State
case object Prepared extends State
case object Committed extends State

sealed trait Data
case object Uninitialized extends Data
case class requestData(responses: Int, currentRequest: String) extends Data
final case class ackData(responses: Int, currentRequest: String) extends Data

class Coordinator(actorVector: Vector[ActorRef]) extends FSM[State, Data] {
  startWith(Idle, Uninitialized)

  when(Idle){
    case Event(Request(request), Uninitialized) =>
      log.info(s"Received Request message: $request")
      goto(Waiting) using requestData(actorVector.size, request)
  }

  when(Waiting){
    case Event(Response(currentRequest,"No"),requestData(_,_)) =>
      log.info(s"Received Response message: No")
      actorVector.foreach(x => x ! Abort(currentRequest))
      goto(Idle) using Uninitialized
    case Event(Response(_,"Yes"),requestData(_,_)) =>
      stateData match {
        case requestData(responses, currentRequest) =>
          log.info(s"Received Response message: Yes, remaining responses: ${responses-1}")
          if(responses - 1 == 0) goto(Prepared) using ackData(actorVector.size, currentRequest)
          else stay using requestData(responses-1, currentRequest)
      }
  }

  when(Prepared){
    case Event(ACK(_),ackData(responses,currentRequest)) =>
      log.info(s"Received ACK message, remaining responses: ${responses-1}")
      if(responses - 1 == 0) goto(Committed)
      else stay using ackData(responses-1, currentRequest)
  }

  when(Committed){
    case Event(Request(request), Uninitialized) =>
      log.info(s"Received Request message: $request")
      goto(Waiting) using requestData(actorVector.size, request)
  }



  onTransition {

    case Idle -> Waiting ⇒
      log.info(s"Transitioning to waiting state and sending out canCommit messages")
      nextStateData match {
      case requestData(_, currentRequest) ⇒
        actorVector.foreach(x => x ! canCommit(currentRequest))
    }

    case Waiting -> Prepared ⇒
      log.info(s"Transitioning to Prepared state and sending out preCommit messages")
      nextStateData match {
        case ackData(_, currentRequest) ⇒
          actorVector.foreach(x => x ! preCommit(currentRequest))
      }

    case Prepared -> Committed ⇒
      log.info(s"Transitioning to Committed state and sending out doCommit messages")
      nextStateData match {
        case ackData(_, currentRequest) ⇒
          actorVector.foreach(x => x ! doCommit(currentRequest))
      }
      log.info(s"Chill and relax, the work has been done")
  }



  initialize()
}


//#main-class
object Coordinator extends App {
  val tpc: ActorSystem = ActorSystem("3PC")
  val firstCenturion = tpc.actorOf(Props(classOf[Cohort]),"firstCenturion")
  val secondCenturion = tpc.actorOf(Props(classOf[Cohort]),"secondCenturion")
  val thirdCenturion = tpc.actorOf(Props(classOf[Cohort]),"thirdCenturion")
  var actorVector: Vector[ActorRef] = Vector(firstCenturion,secondCenturion,thirdCenturion)
  val Ceasar = tpc.actorOf(Props(classOf[Coordinator],actorVector))
  Ceasar ! Request("Brutus")
}