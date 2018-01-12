//#full-example
package com.lightbend.akka.sample

import akka.actor.{ ActorSystem }

//#main-class
object AkkaQuickstart extends App {
  val system: ActorSystem = ActorSystem("helloAkka")
}