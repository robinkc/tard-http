package in.kcrob.tardhttp.jmh

import java.io.{PrintWriter, StringWriter}
import java.util
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, ExecutorService, Executors}

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
class TardHttpBenchmark
  extends Logging {

  //My Static Conf
  val EXPECTED_NETWORK_DELAY = 200

  //Conf passed to me
  val N_PARALLEL_REQ: Int = System.getProperty("N_PARALLEL_REQ").toInt
  val IS_DEBUG: Boolean = System.getProperty("N_PARALLEL_REQ") == "true"
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

  //Jetty Client Setup
  val jettyHttpClient = new HttpClient(new SslContextFactory())
  jettyHttpClient.setFollowRedirects(false)
  jettyHttpClient.start()

  def jettyRequest1: Request = jettyHttpClient.newRequest(requestUrl1)

  //HttpComponentsHttpClientSetup
  val cm = new PoolingHttpClientConnectionManager
  cm.setMaxTotal(N_CONCURRENCY)
  cm.setDefaultMaxPerRoute(N_CONCURRENCY)
  val httpComponentsHttpClient: CloseableHttpClient = HttpClients.custom.setConnectionManager(cm).build
  def httpComponentsRequest1: HttpGet = new HttpGet(requestUrl1)

  //Extra Setup
  val threadPool: ExecutorService = Executors.newFixedThreadPool(N_CONCURRENCY)
  val nErrorsEncountered = new AtomicLong(0)

  @Setup
  def setup(): Unit = {
    println("Setting up")
    var isSuccessful = false
    for(i <- 1 to 10) {
      try{
        benchTardHttpClientWithConcurrentHashMap()
        benchHttpComponentsHttpClient()
        isSuccessful=true
        println("Attempt ", i, "successful")
      }
      catch {
        case _: Throwable => println("Attempt ", i, "failed")
      }
    }

    if(!isSuccessful) {
      throw new RuntimeException("I am unable to successfully invoke even a single thing")
    }

    nErrorsEncountered.set(0)

  }

  @TearDown
  def tearDown(): Unit = {
    println("Tearing Down")
    jettyHttpClient.stop()
    httpComponentsHttpClient.close()
    threadPool.shutdown()
    if(nErrorsEncountered.get() > 0) {
      println("=================")
      println("===== ERRORS = ", nErrorsEncountered.get(), " But remember we do not separate warmup from other iterations?!! so you might have count of warmup iterations")
      println("=================")
    }
    else{
      println("No error")
    }
  }

  @Benchmark
  def benchTardHttpClientWithConcurrentHashMap(): java.util.Map[Int, String] = {
    benchHystrixObservableCommandWithConcurrentHashMap(() => {getHystrixObservableCommand(jettyRequest1)})
  }


  @Benchmark
  def benchHttpComponentsHttpClient(): java.util.Map[Int, String] = {
    benchHystrixBlockingCommand(() => {getHystrixCommand(httpComponentsRequest1)})
  }

//  @Benchmark
  def benchJettyHttpClient(): java.util.Map[Int, String] = {
    benchHystrixBlockingCommand(() => {getHystrixCommand(jettyRequest1)})
  }

  private def benchHystrixObservableCommandWithConcurrentHashMap(commandBuilder: () => HystrixObservableCommand[String]): java.util.Map[Int, String] = {

    //Using ConcurrentHashMap even for a single request, as not expecting any contention, no contention no performance hit
    val map: java.util.Map[Int, String] = new ConcurrentHashMap[Int, String]()

    var response1: String = null

    val latch = new CountDownLatch(N_PARALLEL_REQ)

    for (i <- 0 until N_PARALLEL_REQ) {
      val observable: scala.Observable[String] = commandBuilder().observe()

      observable.subscribe(
        next => {
          map.put(i, next)
        },
        error => {
          handleException(error, map, i)
          latch.countDown()
        },
        () => {
          latch.countDown()
        }
      )
    }

    latch.await()

    verifyResults(map)

    map

  }

  private def benchHystrixBlockingCommand(commandBuilder :() => HystrixCommand[String]): java.util.Map[Int, String] = {

    val map: java.util.Map[Int, String] =     if(N_PARALLEL_REQ == 1) {
      new util.HashMap[Int, String]()
    }
    else{
      new ConcurrentHashMap[Int, String]()
    }

    //We will not create a thread if it is one request
    if(N_PARALLEL_REQ == 1) {
      try {
        map.put(0, commandBuilder().execute())
      }
      catch {
        case error: Throwable => handleException(error, map, 0)
      }
    }

    else {
      val latch = new CountDownLatch(N_PARALLEL_REQ)

      for (i <- 0 until N_PARALLEL_REQ) {
        threadPool.submit(new Runnable {
          override def run(): Unit = {
            try {
              val next: String = commandBuilder().execute()
              map.put(i, next)
            }
            catch {
              case error: Throwable => handleException(error, map, i)
            }
            finally {
              latch.countDown()
            }
          }
        })
      }

      latch.await()
    }

    verifyResults(map)
    map

  }

  //TODO::Remove this
//  @Benchmark
  def benchHttpComponentsHttpClient_old(): java.util.Map[Int, String] = {

    //We will not create a thread if it is one request
    if(N_PARALLEL_REQ == 1) {
      val map = new util.HashMap[Int, String]()

      //      println("Starting Request")

      val next: String = new HttpHystrixCommandForHttpComponents(httpComponentsHttpClient, httpComponentsRequest1, TIMEOUT, N_CONCURRENCY).execute()
      //      println("Ended Request")

      map.put(0, next)

      verifyResults(map)

      map

    }
    else {
      val map = new ConcurrentHashMap[Int, String]()

      val latch = new CountDownLatch(N_PARALLEL_REQ)

      for (i <- 0 until N_PARALLEL_REQ) {
        triggerHttpComponentsHttpClientRequest(i, map, latch)
      }

      latch.await()

      verifyResults(map)

      map
    }

  }

  private def triggerHttpComponentsHttpClientRequest(index: Int, map: java.util.Map[Int, String], latch: CountDownLatch) = {
    triggerHystrixBlockingCommand(index, map, latch, getHystrixCommand(httpComponentsRequest1))
  }

  private def triggerHystrixBlockingCommand(index: Int, map: java.util.Map[Int, String], latch: CountDownLatch, command: HystrixCommand[String]) = {
    threadPool.submit(new Runnable {
      override def run(): Unit = {
        val next: String = command.execute()

        map.put(index, next)

        latch.countDown()
      }
    })
  }


  //#############
  //#############
  //Utility functions
  //#############
  //#############

  //#############
  //Utility functions: Create Commands
  //#############

  private def getHystrixObservableCommand(req: Request): HystrixObservableCommand[String] = {
    new HttpHystrixObservableCommand(req, TIMEOUT, N_CONCURRENCY)
  }

  private def getHystrixCommand(req: Request): HystrixCommand[String] = {
    new HttpHystrixCommand(req, TIMEOUT, N_CONCURRENCY)
  }

  private def getHystrixCommand(request: HttpGet = httpComponentsRequest1): HystrixCommand[String] = {
    new HttpHystrixCommandForHttpComponents(httpComponentsHttpClient, request, TIMEOUT, N_CONCURRENCY)
  }

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

  private def handleException(error: Throwable, map: java.util.Map[Int, String], i: Int) = {
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