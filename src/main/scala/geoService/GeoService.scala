package geoService

import demo.geo._
import io.etcd.jetcd.Election.Listener
import io.etcd.jetcd.election.LeaderResponse
import io.etcd.jetcd.kv.GetResponse
import io.etcd.jetcd.lease.LeaseKeepAliveResponse
import io.etcd.jetcd.options.PutOption
import io.etcd.jetcd.{ByteSequence, Client}
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import scalaj.http.Http
import shade.memcached.{Configuration, Memcached}

import java.net.InetAddress
import scala.collection.JavaConverters
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

class GeoService(cacheURL: String = "memcached:11211", cacheLeaseTime: FiniteDuration) extends GeoServiceGrpc.GeoService {

  // Private methods to resolve the different requests

  private def readCSV: String = {
    val csvString = os.read(os.pwd / "src" / "main" / "scala" / "geoService" / "data.csv")
    csvString
  }

  private def getCountries: String = {
    val countriesList = readCSV.split("\n").map(_.split(",")).map(_ (1)).distinct.tail
    countriesList.map(e => e.replace("\"", "")).mkString(",")
  }

  private def getStates(country: String) = {
    val filteredStatesList = readCSV.split("\n").filter(_.contains(country)).map(_.split(",")).map(_ (2)).distinct
    filteredStatesList.map(e => e.replace("\"", "")).mkString(",")
  }

  private def getCities(state: String) = {
    val filteredCitiesList = readCSV.split("\n").filter(_.contains(state)).map(_.split(",")).map(_ (0)).distinct
    filteredCitiesList.map(e => e.replace("\"", "")).mkString(",")
  }

  val memcached: Memcached = Memcached(Configuration(cacheURL))

  private def getCountryCityByIP(ip: String): GeoReply = {
    var request: GeoReply = GeoReply()
    memcached.awaitGet[Array[Byte]](ip) match {
      case Some(value: Array[Byte]) =>
        request = GeoReply.parseFrom(value)
      case None =>
        val byteArray = GeoReply(Http("http://ipwhois.app/json/" + ip + "?objects=country,region").asString.body).toByteArray
        request = GeoReply.parseFrom(byteArray)
        memcached.awaitAdd(ip, byteArray, cacheLeaseTime) //5 minutes
    }
    request
  }
  // Overridden methods that are exposed to accept requests. They rely on the private methods for execution

  override def getAllCountries(request: GeoPingReq): Future[GeoReply] = {
    val reply = GeoReply(message = getCountries)
    Future.successful(reply)
  }

  override def getAllStates(request: GeoGetStateReq): Future[GeoReply] = {
    val reply = GeoReply(message = getStates(request.country))
    Future.successful(reply)
  }

  override def getAllCities(request: GeoGetCityReq): Future[GeoReply] = {
    val reply = GeoReply(message = getCities(request.city))
    Future.successful(reply)
  }

  override def getCountryCityByIP(request: GeoGetCountryCityByIPReq): Future[GeoReply] = {
    val reply = getCountryCityByIP(request.ip)
    Future.successful(reply)
  }
}

object GeoServer extends App {
  var myLeaseTime: Long = 10
  var cacheURL: String = "memcached:11211"
  var cacheLeaseTime: FiniteDuration = 5.minutes
  var etcdIp: String = "http://etcd:2379"

  val client = Client.builder.endpoints(etcdIp).build
  val clientKV = client.getKVClient
  val clientW = client.getWatchClient
  val ec = client.getElectionClient
  val leaseClient = client.getLeaseClient

  val leaseId: Long = leaseClient.grant(myLeaseTime).get().getID //10

  val localhost: InetAddress = InetAddress.getLocalHost
  val localIpAddress: String = localhost.getHostAddress
  var leader: Boolean = false

  def bytes(str: String): ByteSequence = {
    ByteSequence.from(str.getBytes())
  }

  def registerInETCD(): Unit = {
    val id = "/services/geo/" + localIpAddress
    leaseClient.keepAlive(leaseId, new StreamObserver[LeaseKeepAliveResponse] {
      override def onNext(value: LeaseKeepAliveResponse): Unit = {
        // println("LEASE: " + value)
      }

      override def onError(t: Throwable): Unit = {}

      override def onCompleted(): Unit = {}
    })
    val option = PutOption.newBuilder().withLeaseId(leaseId).build()
    clientKV.put(bytes(id), bytes(localIpAddress), option)
    ec.observe(bytes("/services/geo/election"), new Listener() {
      override def onNext(leaderResponse: LeaderResponse): Unit = {

      }

      override def onError(throwable: Throwable): Unit = {}

      override def onCompleted(): Unit = {}
    })
  }

  def getDataFromETCD(): Unit = {
    val ttl: GetResponse = clientKV.get(bytes("config/services/geo/cache/ttl")).join()
    val url: GetResponse = clientKV.get(bytes("config/services/geo/cache/url")).join()

    cacheLeaseTime = JavaConverters.asScalaBuffer(ttl.getKvs).toList.head.getValue.getBytes.map(_.toChar).mkString.toLong.minutes
    cacheURL = JavaConverters.asScalaBuffer(url.getKvs).toList.head.getValue.getBytes.map(_.toChar).mkString
    println("CACHE URL: " + cacheURL)
    println("CACHE LEASE TIME: " + cacheLeaseTime)
  }

  def watch(): Unit = {
    ec.observe(bytes("/services/geo/election"), new Listener {
      override def onNext(leaderResponse: LeaderResponse): Unit = {
        val leaderIp = leaderResponse.getKv.getValue.getBytes.map(_.toChar).mkString
        leader = leaderIp.equals(localIpAddress)
        println("I am: "+ localIpAddress + " and i am leader: "+ leader)
      }

      override def onError(throwable: Throwable): Unit = println("ERROR ELECTION")

      override def onCompleted(): Unit = println("ON COMPLETED ELECTION")
    })
  }

  val builder = ServerBuilder.forPort(50000)

  getDataFromETCD()

  builder.addService(
    GeoServiceGrpc.bindService(new GeoService(cacheURL, cacheLeaseTime), ExecutionContext.global)
  )

  val server = builder.build()
  server.start()

  //  val ip: String = scala.io.StdIn.readLine(">")
  println("Running.... GEO SERVICE")
  registerInETCD()
  watch()
  while (true) {
    if (!leader) {
      ec.campaign(bytes("/services/geo/election"), leaseId, bytes(localIpAddress))
    }
    Thread.sleep(6000)
  }
  server.awaitTermination()
}
