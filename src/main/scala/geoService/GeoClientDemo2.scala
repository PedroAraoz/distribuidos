package geoService

import demo.geo.{GeoGetCountryCityByIPReq, GeoReply, GeoServiceGrpc}
import io.grpc.ManagedChannelBuilder
import scalaj.http.Http

import java.net.InetAddress
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.io.Source
import scala.util.{Random, Try}
import scala.sys.process._

object GeoClientDemo2 extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global


  def registerInETCD(targetIp: String): Unit = {

    val localhost: InetAddress = InetAddress.getLocalHost
    val localIpAddress: String = localhost.getHostAddress

    println(localIpAddress)
    val curlIP = "http://" + "127.0.0.1:2379" + "/v2/keys/" + "simple" + "/" + localIpAddress
    print(curlIP)
    val cmd = Seq("curl", "-L" ,curlIP,"-XPUT","-d", "value=" + localIpAddress)
    cmd.!
  }
  def createStub(ip: String, port: Int = 50000): GeoServiceGrpc.GeoServiceStub = {
    val builder = ManagedChannelBuilder.forAddress(ip, port)
    builder.usePlaintext()
    val channel = builder.build()

    GeoServiceGrpc.stub(channel)
  }
  def readFile(path: String): List[String] = Source.fromFile(path).getLines.toList
  def recursiveHell(ips: List[String]): Unit = {
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
          throw new RuntimeException("No more healthy stubs")
        recursiveHell(ips)
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
  def handleRequest(path: String): Unit = {
    healthyStubs = Random.shuffle(healthyStubs)
    val ips: List[String] = readFile(path)
    recursiveHell(ips)
  }

  val ip: String = scala.io.StdIn.readLine(">")
  registerInETCD(ip)

  val stub1 = createStub(ip, 50004)
  val stub2 = createStub(ip, 50003)
  val stub3 = createStub(ip, 50002)
  val stub4 = createStub(ip, 50000)

  val stubs = List(stub1, stub2, stub3, stub4)
  var healthyStubs = stubs
  handleRequest("src/main/scala/geoService/ips.txt")
  System.in.read()
}
