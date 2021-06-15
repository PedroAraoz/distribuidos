package geoService

import demo.geo._
import io.etcd.jetcd.Election.Listener
import io.etcd.jetcd.election.LeaderResponse
import io.etcd.jetcd.kv.GetResponse
import io.etcd.jetcd.lease.LeaseKeepAliveResponse
import io.etcd.jetcd.options.PutOption
import io.etcd.jetcd.{ByteSequence, Client}
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannelBuilder, ServerBuilder}
import scalaj.http.Http
import shade.memcached.{Configuration, Memcached}

import java.net.InetAddress
import scala.collection.JavaConverters
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationInt, DurationLong, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

// no andaaaaa

class GeoServiceKub(cacheURL: String = "memcached:11211", cacheLeaseTime: FiniteDuration) extends GeoServiceGrpc.GeoService {
  // Private methods to resolve the different requests

  var leaderStub: GeoServiceGrpc.GeoServiceStub = _

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


object GeoServerKub extends App {
  var cacheURL: String = "10.110.240.30:11211"
  var cacheLeaseTime: FiniteDuration = 5.minutes

  val localhost: InetAddress = InetAddress.getLocalHost
  val localIpAddress: String = localhost.getHostAddress

  val port = sys.env.getOrElse("port", "50000").toInt
 
  val builder = ServerBuilder.forPort(port)

  val service = new GeoServiceKub(cacheURL, cacheLeaseTime)
  builder.addService(GeoServiceGrpc.bindService(service, ExecutionContext.global))

  val server = builder.build()
  server.start()

  println("Running.... GEO SERVICE on port: " + port)
  
  server.awaitTermination()
}
