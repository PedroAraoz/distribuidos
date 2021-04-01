package demo

import demo.msg.{Address, CivilStatus, Person}
import scalapb.json4s.JsonFormat

object Demo extends App {

  val person1 = Person("Juan", 25, CivilStatus.SINGLE, Vector(
    Address("Calle 123,", "CABA")
  ))

  val bytes = Person.toByteArray(person1)
  val person2 = Person.parseFrom(bytes)
  val start = System.currentTimeMillis()

  for (i <- 1 to 100000) {
//    val json = JsonFormat.toJsonString(person1)
//    val person3 = JsonFormat.fromJsonString[Person](json)
    val bytes = Person.toByteArray(person1)
    val person2 = Person.parseFrom(bytes)

  }

  val end = System.currentTimeMillis()

  val elapsed = end - start

  println("Elapsed " + (end - start) + " millis")




//  println("bytes.length: " + bytes.length)
//  println("json.length: " + json.length)
}
