package demo.etcd

import io.etcd.jetcd.options.GetOption
import io.etcd.jetcd.{ByteSequence, Client}

object EtcdDemo extends App {


  def bytes(str: String): ByteSequence = {
    ByteSequence.from(str.getBytes())
  }

  println("Connecting to client....")

  //val endpoint = "http://localhost:2379"
  val endpoint = "http://etcd:2379"

  val client = Client.builder.endpoints(endpoint).build

  val kvClient = client.getKVClient

  val key = bytes("mykey/")
  val key1 = bytes("mykey/123")
  val key2 = bytes("mykey/456")
  val value1 = bytes("myvalue")
  val value2 = bytes("AAAAAAAAAAAAAAAAA")

  // put the key-value
  kvClient.put(key1, value1)
  kvClient.put(key2, value2)


  // get the CompletableFuture
  val option: GetOption = GetOption.newBuilder().withPrefix(key).build()
  val getFuture = kvClient.get(key, option)

  // get the value from CompletableFuture
  val response = getFuture.get

  println(response.getCount)
  response.getKvs.forEach(e => println(e))
  println("0----------------------------0")
  // delete the key
  kvClient.delete(key).get

  println("Completed...")
}
