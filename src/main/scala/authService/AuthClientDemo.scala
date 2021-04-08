package authService

import demo.auth.{AuthReply, AuthReq, AuthServiceGrpc}
import io.grpc.ManagedChannelBuilder

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object AuthClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  def createStub(ip: String, port: Int = 50000): AuthServiceGrpc.AuthServiceStub = {
    val builder = ManagedChannelBuilder.forAddress(ip, port)
    builder.usePlaintext()
    val channel = builder.build()

    AuthServiceGrpc.stub(channel)
  }
  val ip: String = scala.io.StdIn.readLine(">")
  val stub1 = createStub(ip, 50000)
  val stub2 = createStub(ip, 50001)

  val stubs = List(stub1, stub2)
  val healthyStubs = stubs

  // Manual Tests

  //  val response: Future[GeoReply] = stub1.getCountryCityByIP(GeoGetCountryCityByIPReq("181.16.95.204"))

  val response1: Future[AuthReply] =
    stub1.authenticate(AuthReq("juan@gmail.com", "1234"))
  val response2: Future[AuthReply] =
    stub1.authenticate(AuthReq("juan@gmail.com", "9999"))
  val response3: Future[AuthReply] =
    stub1.authenticate(AuthReq("X@gmail.com", "p455w0rd"))

  response1.onComplete { r =>
    println("Response: " + r)
  }
  response2.onComplete { r =>
    println("Response: " + r)
  }
  response3.onComplete { r =>
    println("Response: " + r)
  }

  System.in.read()
}
