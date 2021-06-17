package productService

import demo.product.{ProductReply, ProductReq, ProductServiceGrpc}
import io.grpc.ServerBuilder

import scala.concurrent.{ExecutionContext, Future}

class ProductService extends ProductServiceGrpc.ProductService {

  // A database is mocked through a map in order to store and retrieve the values

  val database: Map[String, Number] = Map(
    ("leche" -> 20),
    ("fideos" -> 10),
    ("papas" -> 30),
  )

  // Private method to resolve the requests

  private def getAll(): String = {
    "[" + database.map(e => s"{product: ${e._1}, price: ${e._2}}").mkString(",") + "]"
  }

  // Overrided method that is exposed to accept requests. It relies on the private method for execution

  override def getProducts(req: ProductReq): Future[ProductReply] = {
    val reply = ProductReply(map = getAll())
    Future.successful(reply)
  }
}

object ProductServer extends App {
  val builder = ServerBuilder.forPort(sys.env.getOrElse("port", "8080").toInt)

  builder.addService(
    ProductServiceGrpc.bindService(new ProductService(), ExecutionContext.global)
  )

  val server = builder.build()
  server.start()

  println("Running....")
  server.awaitTermination()
}