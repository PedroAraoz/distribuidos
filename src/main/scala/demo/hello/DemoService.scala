package demo.hello
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannelBuilder, ServerBuilder}

import scala.concurrent.ExecutionContext.global
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class MyService extends HelloServiceGrpc.HelloService {

  override def sayHello(request: HelloRequest): Future[HelloReply] = {
    val reply = HelloReply(message = "Hello " + request.name)
    Future.successful(reply)
  }

  override def sayHelloMulti(request: HelloRequest, responseObserver: StreamObserver[HelloReply]): Unit = {

    responseObserver.onNext(HelloReply(message = "Hello1 " + request.name))
    responseObserver.onNext(HelloReply(message = "Hello2 " + request.name))
    responseObserver.onNext(HelloReply(message = "Hello3 " + request.name))

    responseObserver.onCompleted()
  }
}

object HelloWorldServer extends App {
  val builder = ServerBuilder.forPort(50000)

  builder.addService(
    HelloServiceGrpc.bindService(new MyService(), ExecutionContext.global)
  )

  val server = builder.build()
  server.start()

  println("Running....")
  server.awaitTermination()
}

object ClientDemo extends App {

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global


  def createStub(ip: String, port: Int = 50000): HelloServiceGrpc.HelloServiceStub = {
    val builder = ManagedChannelBuilder.forAddress(ip, port)
    builder.usePlaintext()
    val channel = builder.build()

    HelloServiceGrpc.stub(channel)
  }

  val stub1 = createStub("127.0.0.1", 50000)
  val stub2 = createStub("127.0.0.1", 50001)

  val stubs = List(stub1, stub2)
  val healthyStubs = stubs



  // Say hello (request/response)
  val response: Future[HelloReply] = stub1.sayHello(HelloRequest("Juan"))

  response.onComplete { r =>
    println("Response: " + r)
  }

  // Say hello (request/response stream)

  stub1.sayHelloMulti(HelloRequest("Juan"), new StreamObserver[HelloReply] {

    override def onNext(value: HelloReply): Unit = {
      println("Response: " + value)
    }
    override def onError(t: Throwable): Unit = {}
    override def onCompleted(): Unit = {}
  })

  System.in.read()
}