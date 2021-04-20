package geoService

import demo.geo.{GeoGetCountryCityByIPReq, GeoReply, GeoServiceGrpc}
import io.etcd.jetcd.options.GetOption
import io.etcd.jetcd.{ByteSequence, Client, KeyValue}
import io.grpc.ManagedChannelBuilder

import scala.collection.JavaConverters
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.io.Source
import scala.util.{Random, Try}


object GeoClientDemo2 extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

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
    futureGeoReplyList.onComplete { r =>
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
      (data("country").toString(), data("region").toString())
    }
    val finalList = tupleList.groupBy(_._1).map(e => (e._1, e._2.map(_._2)))
    print(finalList)
  }

  def handleRequest(path: String): Unit = {
    healthyStubs = Random.shuffle(healthyStubs)
    val ips: List[String] = readFile(path)
    recursiveHell(ips)
  }

  def bytes(str: String): ByteSequence = {
    ByteSequence.from(str.getBytes())
  }

  def getServiceIps(etcdIp: String = "http://etcd:2379"): List[String] = {
    val client = Client.builder.endpoints(etcdIp).build
    val kVClient = client.getKVClient
    val prefix = bytes("/services/geo/")
    val option = GetOption.newBuilder().withPrefix(prefix).build()
    val getFuture = kVClient.get(prefix, option) //todo esto deberia ser un prefix
    val response = getFuture.get
    val watchClient = client.getWatchClient
//    watchClient.watch(bytes("/services/geo/"))
//    etcdctl watch -- prefix service / geo //todo pasar a scala
    val almost = JavaConverters.asScalaBuffer(response.getKvs).toList
    almost.map(_.getValue.toString())
  }

  //  asumimos que va a ser algo asi
//  def notifyDeathOfService(key: String): Unit = {
//    ips.filterNot(k => k.equals(client.get(key)))
//  }

  val ips: List[String] = getServiceIps()
  val circularIps = Iterator.continually(ips).flatten
  val stub1 = createStub(circularIps.next(), 50004) //falta testear lo de los puertos en vivo
  val stub2 = createStub(circularIps.next(), 50003)
  val stub3 = createStub(circularIps.next(), 50002)
  val stub4 = createStub(circularIps.next(), 50000)

  val stubs = List(stub1, stub2, stub3, stub4)
  var healthyStubs = stubs
  handleRequest("src/main/scala/geoService/ips.txt")
  System.in.read()
}
