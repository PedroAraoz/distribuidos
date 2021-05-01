package geoService

import demo.cache.GeoData
import demo.geo._
import io.etcd.jetcd.Election.Listener
import io.etcd.jetcd.election.{CampaignResponse, LeaderResponse}
import io.etcd.jetcd.kv.GetResponse
import io.etcd.jetcd.lease.LeaseKeepAliveResponse
import io.etcd.jetcd.options.PutOption
import io.etcd.jetcd.{ByteSequence, Client}
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import scalaj.http.Http
import shade.memcached.{Configuration, Memcached}

import java.net.InetAddress
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt, DurationLong, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

  private def getCountryCityByIP(ip: String): String = {
    val result: Future[Option[Array[Byte]]] = memcached.get[Array[Byte]](ip)
    var request: String = ""
    val asd: Try[Option[Array[Byte]]] = Await.ready(result, Duration.Inf).value.get
    asd match {
      case Success(value) =>
        if (value.isDefined) {
          request = GeoData.parseFrom(value.get).toProtoString
          println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAa")
          println(request)
          println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAa")
        }
        else {
          request = Http("http://ipwhois.app/json/" + ip + "?objects=country,region").asString.body
          memcached.set(ip, jsonToGeoData(request).toByteArray, cacheLeaseTime) //5 minutes
        }
      case Failure(_) => throw new RuntimeException
    }
    request
  }
  // Overridden methods that are exposed to accept requests. They rely on the private methods for execution

  def jsonToGeoData(json: String): GeoData = {
    println("JSONTOGEODATA")
    println(json)
    val data = ujson.read(json)
    GeoData(data("country").toString(), data("region").toString())
  }

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
    val reply = GeoReply(message = getCountryCityByIP(request.ip))
    Future.successful(reply)
  }
}

object GeoServer extends App {
  var myLeaseTime: Long = 10
  var cacheURL: String = "memcached:11211"
  var cacheLeaseTime: FiniteDuration = 5.minutes
  def bytes(str: String): ByteSequence = {
    ByteSequence.from(str.getBytes())
  }

  def registerInETCD(etcdIp: String = "http://etcd:2379"): Unit = {
    val localhost: InetAddress = InetAddress.getLocalHost
    val localIpAddress: String = localhost.getHostAddress
    println(localIpAddress)
    val client = Client.builder.endpoints(etcdIp).build


    val ec = client.getElectionClient

    val kVClient = client.getKVClient
    val leaseClient = client.getLeaseClient
    val id = "/services/geo/" + localIpAddress

    val leaseId: Long = leaseClient.grant(myLeaseTime).get().getID //10
    leaseClient.keepAlive(leaseId, new StreamObserver[LeaseKeepAliveResponse] {
      override def onNext(value: LeaseKeepAliveResponse): Unit = {
        // println("LEASE: " + value)
      }

      override def onError(t: Throwable): Unit = {}

      override def onCompleted(): Unit = {}
    })
    val option = PutOption.newBuilder().withLeaseId(leaseId).build()
    kVClient.put(bytes(id), bytes(localIpAddress), option)
    ec.observe(bytes("/services/geo/election"), new Listener() {
      override def onNext(leaderResponse: LeaderResponse): Unit = {

      }

      override def onError(throwable: Throwable): Unit = {}

      override def onCompleted(): Unit = {}
    })
  }

  def getDataFromETCD(etcdIp: String = "http://etcd:2379"): Unit = {
    val client = Client.builder.endpoints(etcdIp).build
    val clientKV = client.getKVClient

    val clientW = client.getWatchClient
    //todo completar
    val ttl: GetResponse = clientKV.get(bytes("config/services/geo/cache/ttl")).get
    val url: GetResponse = clientKV.get(bytes("config/services/geo/cache/url")).get
//    cacheURL = url.getKvs.get(0).getValue.toString
//    cacheLeaseTime = ttl.getKvs.get(0).getValue.toString().toLong.minutes
  }

  val builder = ServerBuilder.forPort(50000)

  builder.addService(
    GeoServiceGrpc.bindService(new GeoService(cacheURL, cacheLeaseTime), ExecutionContext.global)
  )

  val server = builder.build()
  server.start()

  //  val ip: String = scala.io.StdIn.readLine(">")
  println("Running.... GEO SERVICE")
  getDataFromETCD()
  registerInETCD()

//  while (true) {
//    if (!leader)
//      ec.campaign(bytes("/services/geo/election"), leaseId, bytes(localIpAddress))
//    Thread.sleep(6000)
//  }
  server.awaitTermination()
}
