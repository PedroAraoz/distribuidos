package authService

import demo.auth.{AuthReply, AuthReq, AuthServiceGrpc, AuthStatus}

import scala.concurrent.Future

class AuthService extends AuthServiceGrpc.AuthService {

  private def getAuthentication(mail: String, pasword: String): AuthStatus = {
    null
  }

  override def authenticate (req: AuthReq): Future[AuthReply] = {
    val reply = AuthReply(status = getAuthentication(req.mail, req.password))
    Future.successful(reply)
  }
}
