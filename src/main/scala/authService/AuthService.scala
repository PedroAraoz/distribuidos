package authService

import demo.auth.{AuthReply, AuthReq, AuthServiceGrpc, AuthStatus}
import io.grpc.{ManagedChannelBuilder, ServerBuilder}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class AuthService extends AuthServiceGrpc.AuthService {

  // A database is mocked through a map in order to store and retrieve the values

  val database: Map[String, String] = Map(
    ("juan@gmail.com" -> "1234"),
    ("maria@gmail.com" -> "maria"),
    ("pepe@gmail.com" -> "password"),
  )

  // Private method to resolve the requests

  private def getAuthentication(mail: String, password: String): AuthStatus = {
    if (database.contains(mail))
      if (database(mail).equals(password)) AuthStatus.OK else AuthStatus.FAILURE
    else AuthStatus.FAILURE
  }

  // Overrided method that is exposed to accept requests. It relies on the private method for execution

  override def authenticate(req: AuthReq): Future[AuthReply] = {
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
