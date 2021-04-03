package authService

import demo.auth.{AuthReply, AuthReq, AuthServiceGrpc, AuthStatus}
import io.grpc.{ManagedChannelBuilder, ServerBuilder}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class AuthService extends AuthServiceGrpc.AuthService {
  val database: Map[String, String] = Map(
    ("juan@gmail.com" -> "1234") ,
    ("maria@gmail.com" -> "maria") ,
    ("pepe@gmail.com" -> "password") ,
  )
  private def getAuthentication(mail: String, password: String): AuthStatus = {
    if (database.contains(mail))
      if (database(mail).equals(password)) AuthStatus.OK else AuthStatus.FAILURE
    else AuthStatus.FAILURE
  }

  override def authenticate (req: AuthReq): Future[AuthReply] = {
    val reply = AuthReply(status = getAuthentication(req.mail, req.password))
    Future.successful(reply)
  }
}


object AuthServer extends App {
  val builder = ServerBuilder.forPort(50000)

  builder.addService(
    AuthServiceGrpc.bindService(new AuthService(), ExecutionContext.global)
  )

  val server = builder.build()
  server.start()

  println("Running....")
  server.awaitTermination()
}


object ClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global


  def createStub(ip: String, port: Int = 50000): AuthServiceGrpc.AuthServiceStub = {
    val builder = ManagedChannelBuilder.forAddress(ip, port)
    builder.usePlaintext()
    val channel = builder.build()

    AuthServiceGrpc.stub(channel)
  }

  val stub1 = createStub("127.0.0.1", 50000)
  val stub2 = createStub("127.0.0.1", 50001)

  val stubs = List(stub1, stub2)
  val healthyStubs = stubs


//  val response: Future[GeoReply] = stub1.getCountryCityByIP(GeoGetCountryCityByIPReq("181.16.95.204"))
  val response1: Future[AuthReply] =
    stub1.authenticate(AuthReq("juan@gmail.com","1234"))
  val response2: Future[AuthReply] =
    stub1.authenticate(AuthReq("juan@gmail.com","9999"))
  val response3: Future[AuthReply] =
    stub1.authenticate(AuthReq("X@gmail.com","p455w0rd"))
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
