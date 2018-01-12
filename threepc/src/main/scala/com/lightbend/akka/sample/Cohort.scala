//#full-example
package com.lightbend.akka.sample

import akka.actor.{FSM}


class Cohort extends FSM[State, Data]{
  startWith(Idle, Uninitialized)

  when(Idle){
    case Event(canCommit(request), _) =>
      log.info("Received canCommit message")
      Thread.sleep(1000)
      sender() ! Response(request,"Yes")
      goto(Waiting)
  }

  when(Waiting){
    case Event(preCommit(request), _) =>
      log.info("Received preCommit message")
      Thread.sleep(1000)
      sender() ! ACK(request)
      goto(Prepared)
  }

  when(Prepared){
    case Event(doCommit(_), _) =>
      log.info("Received doCommit message")
      Thread.sleep(1000)
      goto(Committed)
  }

  when(Committed){
    case Event(canCommit(request), _) =>
      log.info("Received canCommit message")
      Thread.sleep(1000)
      sender() ! Response(request,"Yes")
      goto(Waiting)
  }
}
