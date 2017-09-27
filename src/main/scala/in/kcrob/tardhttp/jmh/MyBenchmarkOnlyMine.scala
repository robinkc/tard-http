package in.kcrob.tardhttp.jmh

import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, ExecutorService, Executors}

import in.kcrob.tardhttp.{HttpHystrixCommand, HttpHystrixObservableCommand}
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.openjdk.jmh.annotations.{Benchmark, Scope, State, TearDown}
import rx.lang.scala
import in.kcrob.tardhttp.HttpHystrixObservableCommand
import rx.lang.scala
import rx.lang.scala.JavaConversions._

/**
  * Created by kcrob.in on 27/09/17.
  */

@State(Scope.Benchmark)
class MyBenchmarkOnlyMine {

  val httpClient = new HttpClient(new SslContextFactory())
  httpClient.setFollowRedirects(false)
  httpClient.start()

  val baseUrl = "http://localhost:8080"
  def req1: Request = httpClient.newRequest(s"$baseUrl/hello")

  val n = 7
  val threadPool: ExecutorService = Executors.newFixedThreadPool(n)

  @TearDown
  def tearDown(): Unit = {
    println("Tearing Down")
    httpClient.stop()
    threadPool.shutdown()
  }

  @Benchmark
  def benchHttpHystrixObservableCommand(): java.util.Map[Int, String] = {
    val map = new ConcurrentHashMap[Int, String]()

    val latch = new CountDownLatch(n)

    for(i <- 0 to n) {
      createNewObservableCommand(i, map, latch)
    }

    latch.await()

    map
  }

  @Benchmark
  def benchHttpHystrixCommand(): java.util.Map[Int, String] = {
    val map = new ConcurrentHashMap[Int, String]()

    val latch = new CountDownLatch(n)

    for(i <- 0 to n) {
      createNewCommand(i, map, latch)
    }

    latch.await()

    map
  }

  private def createNewObservableCommand(index: Int, map: java.util.Map[Int, String], latch: CountDownLatch) = {
    val command: scala.Observable[String] = new HttpHystrixObservableCommand(req1).observe()

    command.subscribe(
      next => {
        if (next != "world\n") {
          println("No!!!!!!")
          throw new RuntimeException("No!!!")
        }
        map.put(index, next)
      },
      error => {
        latch.countDown()
      },
      () => {
        latch.countDown()
      }
    )
  }

  private def createNewCommand(index: Int, map: java.util.Map[Int, String], latch: CountDownLatch) = {

    threadPool.submit(new Runnable {
      override def run(): Unit = {
        val next: String = new HttpHystrixCommand(req1).execute()

        if (next != "world\n") {
          println("No!!!!!!")
          throw new RuntimeException("No!!!")
        }
        map.put(index, next)

        latch.countDown()
      }
    })
  }
}