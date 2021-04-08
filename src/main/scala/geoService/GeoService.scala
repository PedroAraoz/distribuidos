package geoService

import demo.geo._
import io.grpc.ServerBuilder
import scalaj.http.Http

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
  val builder = ServerBuilder.forPort(50000)

  builder.addService(
    GeoServiceGrpc.bindService(new GeoService(), ExecutionContext.global)
  )

  val server = builder.build()
  server.start()

  println("Running....")
  server.awaitTermination()
}
