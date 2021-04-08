package geoService

import demo.geo.{GeoGetCountryCityByIPReq, GeoReply, GeoServiceGrpc}
import io.grpc.ManagedChannelBuilder

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
import scala.io.Source
import scala.util.Try

object GeoClientDemo2 extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def createStub(ip: String, port: Int = 50000): GeoServiceGrpc.GeoServiceStub = {
    val builder = ManagedChannelBuilder.forAddress(ip, port)
    builder.usePlaintext()
    val channel = builder.build()

    GeoServiceGrpc.stub(channel)
  }
  def readFile(): List[String] = Source.fromFile("src/main/scala/geoService/ips.txt").getLines.toList
  val ip: String = scala.io.StdIn.readLine(">")
//  val ip = "localhost"
  var ips: List[String] = readFile()


  val stub1 = createStub(ip, 50004)
  val stub2 = createStub(ip, 50003)
  val stub3 = createStub(ip, 50002)
  val stub4 = createStub(ip, 50000)

  val stubs = List(stub1, stub2, stub3, stub4)
  var healthyStubs = stubs
  // POLICY = PICK_FIRST
  def recursiveHell(): Unit = {
    val responses: List[Future[GeoReply]] = ips.map {
      e =>
        healthyStubs.head.getCountryCityByIP(GeoGetCountryCityByIPReq(e))
    }
    val futureGeoReplyList: Future[List[GeoReply]] = Future.sequence(responses)
    futureGeoReplyList.onComplete{ r =>
      if (r.isFailure) {
        println(healthyStubs.head + " is dead, removing from list")
        healthyStubs = healthyStubs.tail
        if (healthyStubs.isEmpty)
          healthyStubs = stubs
        recursiveHell()
      } else {
        println("Ding ding ding")
        parse(r)
      }
    }
  }

  def parse(r: Try[List[GeoReply]]): Unit = {
    val tupleList = r.get.map { e =>
      val data = ujson.read(e.message)
      (data("country").toString() , data("region").toString())
    }
    val finalList = tupleList.groupBy(_._1).map(e => (e._1, e._2.map(_._2)))
    print(finalList)
  }
  recursiveHell()

  System.in.read()
}
