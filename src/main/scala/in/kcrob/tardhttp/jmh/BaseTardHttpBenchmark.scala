package in.kcrob.tardhttp.jmh

import java.io.{PrintWriter, StringWriter}
import java.util
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent._

import com.netflix.hystrix.{HystrixCommand, HystrixObservableCommand}
import in.kcrob.scalacommon.Logging
import in.kcrob.tardhttp.{HttpHystrixCommand, HttpHystrixCommandForHttpComponents, HttpHystrixObservableCommand}
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.openjdk.jmh.annotations._
import rx.lang.scala
import rx.lang.scala.JavaConversions._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

/**
  * Created by kcrob.in on 27/09/17.
  */

@State(Scope.Benchmark)
abstract class BaseTardHttpBenchmark
  extends Logging {

  //My Static Conf
  val EXPECTED_NETWORK_DELAY = 200

  //Conf passed to me
  val N_PARALLEL_REQ: Int = System.getProperty("N_PARALLEL_REQ").toInt
  val IS_DEBUG: Boolean = "true" == System.getProperty("IS_DEBUG")
  val SERVER_REQUEST_URI: String = System.getProperty("SERVER_REQUEST_URI").toString

  val requestUrl1: String = {
    val SERVER_BASE_URL: String = System.getProperty("SERVER_BASE_URL").toString
    s"$SERVER_BASE_URL/$SERVER_REQUEST_URI"
  }

  //Lets find concurrency
  val N_CONCURRENCY: Int = {
    val N_THREADS: Int = System.getProperty("N_THREADS").toInt
    N_THREADS * N_PARALLEL_REQ
  }

  //lets identify timeout
  val TIMEOUT: Int = {
    val serverDelay = SERVER_REQUEST_URI.substring("hello_".length).toInt
    serverDelay + EXPECTED_NETWORK_DELAY
  }

//  val jettyBlockingHttpClient = new HttpClient(new SslContextFactory())
//  jettyBlockingHttpClient.setConnectBlocking(true)
//  jettyBlockingHttpClient.setMaxConnectionsPerDestination(N_CONCURRENCY)
//  jettyBlockingHttpClient.setFollowRedirects(false)
//  jettyBlockingHttpClient.start()

//  def jettyRequest2: Request = jettyBlockingHttpClient.newRequest(requestUrl1)

  val nErrorsEncountered = new AtomicLong(0)

  @Setup
  def setup(): Unit = {
    println("Setting up")
    var wasAnySuccessful = false
    for(i <- 1 to 10) {
      try{
        setup0()
        wasAnySuccessful=true
        println("Attempt ", i, "successful")
      }
      catch {
        case _: Throwable =>
          println("Attempt ", i, "failed")
      }
    }

    if(!wasAnySuccessful) {
      throw new RuntimeException("I am unable to successfully invoke even a single thing")
    }

    nErrorsEncountered.set(0)

  }

  def setup0(): Unit

  def tearDown(): Unit = {
    println("Tearing Down")
    if(nErrorsEncountered.get() > 0) {
      println("=================")
      println("===== ERRORS = ", nErrorsEncountered.get(), " But remember we do not separate warmup from other iterations?!! so you might have count of warmup iterations")
      println("=================")
    }
    else{
      println("No error")
    }
  }

//  val hystrixBlockingJettyBlockingCommandBuilder: () => HystrixCommand[String] = () => {
//    getHystrixCommand(jettyRequest2)
//  }


//    @Benchmark
//  def benchJettyHttpClient(): java.util.Map[Int, String] = {
//    benchHystrixBlockingCommand(hystrixJettyBlockingCommandBuilder)
//  }
//
//    @Benchmark
//  def benchBlockingJettyHttpClient(): java.util.Map[Int, String] = {
//    benchHystrixBlockingCommand(hystrixBlockingJettyBlockingCommandBuilder)
//  }


  //#############
  //#############
  //Utility functions
  //#############
  //#############

  //#############
  //Utility functions: Create Commands
  //#############

  //#############
  //Utility functions: Verify Results
  //#############


  private def verifyResults (map: java.util.Map[Int, String]): Unit = {
    for(i <- 0 until N_PARALLEL_REQ) {

      val str = map.get(i)
      if(str != "world\n") {
        throw new RuntimeException("No!!!, we got '" + str + "'")
      }
    }
  }

  private def verifyResults (results: Array[String]): Unit = {
    for(i <- 0 until N_PARALLEL_REQ) {
      if(results(i) != "world\n") {
        throw new RuntimeException("No!!")
      }
    }
  }

  //#############
  //Utility functions: Exception Handling
  //#############

  protected def handleException(error: Throwable, map: java.util.Map[Int, String], i: Int) = {
    if(IS_DEBUG) {
      val sw = new StringWriter
      val printWriter = new PrintWriter(sw)
      error.printStackTrace(printWriter)
      error.getCause.printStackTrace(printWriter)
      map.put(i, sw.toString)
    }
    else {
      map.put(i, "world\n")
    }

    nErrorsEncountered.incrementAndGet()
  }



  //  @Benchmark
  //  def benchTardHttpClientWithArray(): Array[String] = {
  //
  //    var results = new Array[String](N_PARALLEL_REQ)
  //
  //    val latch = new CountDownLatch(N_PARALLEL_REQ)
  //
  //    for(i <- 0 until N_PARALLEL_REQ) {
  //      triggerTardHttpClientRequest(i, results, latch)
  //    }
  //
  //    latch.await()
  //
  //    verifyResults(results)
  //
  //    results
  //  }

  //  @Benchmark
  //  def benchJettyHttpClient(): java.util.Map[Int, String] = {
  //
  //    //We will not create a thread if it is one request
  //    if(N_PARALLEL_REQ == 1) {
  //      val map = new util.HashMap[Int, String]()
  //
  //      //      println("Starting Request")
  //      val next: String = new HttpHystrixCommand(jettyRequest1).execute()
  //      //      println("Ended Request")
  //
  //      map.put(0, next)
  //
  //      verifyResults(map)
  //
  //      map
  //
  //    }
  //    else {
  //      val map = new ConcurrentHashMap[Int, String]()
  //
  //      val latch = new CountDownLatch(N_PARALLEL_REQ)
  //
  //      for (i <- 0 until N_PARALLEL_REQ) {
  //        triggerJettyHttpClientRequest(i, map, latch)
  //      }
  //
  //      latch.await()
  //
  //      verifyResults(map)
  //
  //      map
  //    }
  //
  //  }



}