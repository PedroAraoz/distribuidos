package geoService

import demo.geo.GeoServiceGrpc.GeoService
import demo.geo.{GeoGetCityReq, GeoGetStateReq, GeoPingReq, GeoReply, GeoServiceGrpc}
import io.grpc.{ManagedChannelBuilder, ServerBuilder}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class GeoService extends GeoServiceGrpc.GeoService {
  private def readCSV(): String = {
    val csvString = os.read(os.pwd/"src"/"main"/"scala"/"geoService"/"data.csv")
    csvString
  }

  private def getCountries(): String = {
    val asd = readCSV().split("\n").map(_.split(",")).map(_(1)).distinct.tail
    asd.map(e => e.replace("\"", "")).mkString(",")
  }

  private def getStates(country: String) = {
    val asd = readCSV().split("\n").filter(_.contains(country)).map(_.split(",")).map(_(2)).distinct.tail
    asd.map(e => e.replace("\"", "")).mkString(",")
  }

  private def getCities(state: String) = {
    val asd = readCSV().split("\n").filter(_.contains(state)).map(_.split(",")).map(_(0)).distinct.tail
    asd.map(e => e.replace("\"", "")).mkString(",")
  }

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

object ClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global


  def createStub(ip: String, port: Int = 50000): GeoServiceGrpc.GeoServiceStub = {
    val builder = ManagedChannelBuilder.forAddress(ip, port)
    builder.usePlaintext()
    val channel = builder.build()

    GeoServiceGrpc.stub(channel)
  }

  val stub1 = createStub("127.0.0.1", 50000)
  val stub2 = createStub("127.0.0.1", 50001)

  val stubs = List(stub1, stub2)
  val healthyStubs = stubs

  // Say hello (request/response)
//  val response: Future[GeoReply] = stub1.getAllStates(GeoGetStateReq("Argentina"))
  val response: Future[GeoReply] = stub1.getAllCities(GeoGetCityReq("Paris"))

  response.onComplete { r =>
    println("Response: " + r)
  }

  System.in.read()
}
