package geoService

import demo.geo.{GeoGetCountryCityByIPReq, GeoReply, GeoServiceGrpc}
import io.etcd.jetcd.Watch.Listener
import io.etcd.jetcd.options.{GetOption, WatchOption}
import io.etcd.jetcd.watch.WatchEvent.EventType
import io.etcd.jetcd.watch.WatchResponse
import io.etcd.jetcd.{ByteSequence, Client}
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
//  def readFile(path: String): List[String] = {
//    //todo fix. hardcodeado para no trabarnos con esto
//    List("181.46.160.108", "1.1.1.1", "181.165.170.164", "181.46.160.108")
//  }

  def recursiveHell(ips: List[String]): Unit = {
    val responses: List[Future[GeoReply]] = ips.map {
      e => {
        Thread.sleep(500);
        healthyStubs.head.getCountryCityByIP(GeoGetCountryCityByIPReq(e))
      }
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
    println("Processing...")
    val finalList = tupleList.groupBy(_._1).map(e => (e._1, e._2.map(_._2)))
    println(finalList)
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
    val getFuture = kVClient.get(prefix, option)
    val response = getFuture.get
    val watchClient = client.getWatchClient
    val watchOption = WatchOption.newBuilder().withPrefix(bytes("/services/geo/")).build()
    watchClient.watch(bytes("/services/geo/"), watchOption, new Listener {
      override def onNext(watchResponse: WatchResponse): Unit = {
        val a = JavaConverters.asScalaBuffer(watchResponse.getEvents).toList
        a.filter(_.getEventType.eq(EventType.DELETE)).foreach {
          e =>
            val ip = e.getKeyValue.getValue.getBytes.map(_.toChar).mkString
            ips = ips.filterNot(_.eq(ip))
        }
      }

      override def onError(throwable: Throwable): Unit = println("ERROR")

      override def onCompleted(): Unit = println("COMPLETED!")
    })
//    etcdctl watch -- prefix service / geo //todo pasar a scala
    JavaConverters.asScalaBuffer(response.getKvs).toList.map(_.getValue.getBytes.map(_.toChar).mkString)
  }


  var ips: List[String] = getServiceIps()
  val circularIps = Iterator.continually(ips).flatten
  val stub1 = createStub(circularIps.next(), 50004) //falta testear lo de los puertos en vivo
  val stub2 = createStub(circularIps.next(), 50003)
  val stub3 = createStub(circularIps.next(), 50002)
  val stub4 = createStub(circularIps.next(), 50000)

  val stubs = List(stub1, stub2, stub3, stub4)
  var healthyStubs = stubs
  handleRequest("/txts/ips.txt")

  print("GEOCLIENT:")

  System.in.read()
}
