package geoService

import demo.geo.{GeoGetCountryCityByIPReq, GeoReply, GeoServiceGrpc}
import io.grpc.ManagedChannelBuilder

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object GeoClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def createStub(ip: String, port: Int = 50000): GeoServiceGrpc.GeoServiceStub = {
    val builder = ManagedChannelBuilder.forAddress(ip, port)
    builder.usePlaintext()
    val channel = builder.build()

    GeoServiceGrpc.stub(channel)
  }

  val ip: String = scala.io.StdIn.readLine(">")

  val stub1 = createStub(ip, 50000)
  val stub2 = createStub(ip, 50001)

  val stubs = List(stub1, stub2)
  val healthyStubs = stubs

  // Manual Tests

  //  val response: Future[GeoReply] = stub1.getAllStates(GeoGetStateReq("Argentina"))

  //  val response: Future[GeoReply] = stub1.getAllCities(GeoGetCityReq("Arizona"))

  val response: Future[GeoReply] = stub1.getCountryCityByIP(GeoGetCountryCityByIPReq("181.16.95.204"))
  Thread.sleep(10000) // Due to being Asynchronous
  val response2: Future[GeoReply] = stub1.getCountryCityByIP(GeoGetCountryCityByIPReq("181.16.95.204"))

  response.onComplete { r =>
    println("Response: " + r)
  }

  System.in.read()
}
