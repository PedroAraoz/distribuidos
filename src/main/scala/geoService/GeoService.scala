package geoService

import demo.geo._
import io.etcd.jetcd.lease.LeaseKeepAliveResponse
import io.etcd.jetcd.options.PutOption
import io.etcd.jetcd.{ByteSequence, Client}
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver

import java.util.concurrent.CompletableFuture
//import jetcd.EtcdClientFactory
import scalaj.http.Http

import java.net.InetAddress
import scala.concurrent.{ExecutionContext, Future}

class GeoService extends GeoServiceGrpc.GeoService {

  // There is a cache per instance of the GeoService. It saves the looked up ips so it doesn't have to re-make the request
  var cache: Map[String, String] = Map()

  // Private methods to resolve the different requests

  private def readCSV(): String = {
    val csvString = os.read(os.pwd / "src" / "main" / "scala" / "geoService" / "data.csv")
    csvString
  }

  private def getCountries(): String = {
    val countriesList = readCSV().split("\n").map(_.split(",")).map(_ (1)).distinct.tail
    countriesList.map(e => e.replace("\"", "")).mkString(",")
  }

  private def getStates(country: String) = {
    val filteredStatesList = readCSV().split("\n").filter(_.contains(country)).map(_.split(",")).map(_ (2)).distinct
    filteredStatesList.map(e => e.replace("\"", "")).mkString(",")
  }

  private def getCities(state: String) = {
    val filteredCitiesList = readCSV().split("\n").filter(_.contains(state)).map(_.split(",")).map(_ (0)).distinct
    filteredCitiesList.map(e => e.replace("\"", "")).mkString(",")
  }

  private def getCountryCityByIP(ip: String): String = {

    if (cache.contains(ip)) {
      cache(ip)
    } else {
      val request = Http("http://ipwhois.app/json/" + ip + "?objects=country,region").asString.body
      cache += (ip -> request)
      request
    }

  }

  // Overrided methods that are exposed to accept requests. They rely on the private methods for execution

  override def getAllCountries(request: GeoPingReq): Future[GeoReply] = {
    val reply = GeoReply(message = getCountries())
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
  def bytes(str: String): ByteSequence = {
    ByteSequence.from(str.getBytes())
  }

  def registerInETCD(etcdIp: String = "http://etcd:2379"): Unit = {
    val localhost: InetAddress = InetAddress.getLocalHost
    val localIpAddress: String = localhost.getHostAddress
    println(localIpAddress)
    val client = Client.builder.endpoints(etcdIp).build
    val kVClient = client.getKVClient
    val leaseClient = client.getLeaseClient
    val id = "/services/geo/" + localIpAddress

    val leaseId: Long = leaseClient.grant(10).get().getID
//    leaseClient.keepAlive(leaseId, new StreamObserver[LeaseKeepAliveResponse] {
//      override def onNext(value: LeaseKeepAliveResponse): Unit = println("LEASE: " + value)
//      override def onError(t: Throwable): Unit = {}
//      override def onCompleted(): Unit = {}
//    })
    val option = PutOption.newBuilder().withLeaseId(leaseId).build()
    kVClient.put(bytes(id), bytes(localIpAddress), option)
    //  se supone que la renovacion es automatica, pero como no podemos testear anda a saber.
  }

  val builder = ServerBuilder.forPort(50000)

  builder.addService(
    GeoServiceGrpc.bindService(new GeoService(), ExecutionContext.global)
  )

  val server = builder.build()
  server.start()

  //  val ip: String = scala.io.StdIn.readLine(">")
  println("Running.... GEO SERVICE")
  registerInETCD()
  server.awaitTermination()
}
