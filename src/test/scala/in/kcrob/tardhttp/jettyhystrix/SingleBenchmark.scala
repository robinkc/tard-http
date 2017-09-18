package in.kcrob.tardhttp.jettyhystrix

import java.util.concurrent.CountDownLatch

import in.kcrob.tardhttp.{TardHttpClient, UnitSpec}
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.{Request, Response}
import org.eclipse.jetty.util.ssl.SslContextFactory
import rx.lang.scala
import rx.lang.scala.JavaConversions._

/**
  * Created by kcrob.in on 14/08/17.
  */


class SingleBenchmark
  extends UnitSpec{

  def time[R](block: => R): (R, Long) = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    val timeSpent: Long = t1-t0
    (result, timeSpent)
  }

  def benchmark[R] (block: => R, message: String) = {
    LOG.info("Starting test")
    val out = time {
      for (i <- 1 to LOOPS) {
        block
      }
    }

    val nanos = out._2
    val micros = nanos / 1000
    val millis = micros / 1000
    val secs = millis / 1000
    LOG.info (s"Time taken to perform $LOOPS in $message = ${nanos} nanoseconds which is $micros microseconds $millis millis which is $secs seconds")

  }

  var httpClient: HttpClient = _

  before {
    httpClient = new HttpClient(new SslContextFactory())
    httpClient.setFollowRedirects(false)
    httpClient.start()
  }

  after {
    httpClient.stop()
  }

//  def req1: Request = httpClient.newRequest("http://foaas.com/cool/robin")
//    .header("Accept", "text/plain")
//
//  def req2 = httpClient.newRequest("http://foaas.com/bm/robin/chugh")
//    .header("Accept", "text/plain")
//
  def req1: Request = httpClient.newRequest("http://localhost:8080/hello")

  def req2 = httpClient.newRequest("http://foaas.com/bm/robin/chugh")
    .header("Accept", "text/plain")


  val LOOPS = 1000

  def blockingSanity() = {
    val command = new HttpHystrixCommand(req1)
    val response = command.execute()
    response shouldBe "Cool story, bro. - robin"
  }


  def nonBlockingSanity() = {
    val latch = new CountDownLatch(1)
    var response1: String = ""

    val command: scala.Observable[String] = new HttpHystrixObservableCommand(req1).toObservable

    command.subscribe(
      next => {
        response1 = next
      },
      error => {
        latch.countDown()
      },
      () => {
        latch.countDown()
      }
    )

    latch.await()
    response1 shouldBe "Cool story, bro. - robin"
  }

  def blockingSanityResponse() = {
    val command = new HttpHystrixCommandContentResponse(req1)
    val response = command.execute()
    response.getStatus shouldBe 200
  }

  def nonBlockingSanityResponse() = {
    val latch = new CountDownLatch(1)
    var response1: Response = null

    val command: scala.Observable[Response] = new HttpHsytrixObservableCommandContentResponse(req1).toObservable

    command.subscribe(
      next => {
        response1 = next
      },
      error => {
        latch.countDown()
      },
      () => {
        latch.countDown()
      }
    )

    latch.await()
    response1.getStatus shouldBe 200
  }

  describe("Blocking Mode") {
    it ("sanity check") {
      blockingSanity()
    }

    it ("benchmark") {
      benchmark ({
        blockingSanity()
      }, "blocking")
    }
  }

  describe("non blocking mode") {
    it("Sanity Check") {
      nonBlockingSanity()
    }
    it("benchmark") {
      benchmark ({
        nonBlockingSanity()
      }, "non-blocking")
    }
  }

  describe ("Checking") {
    it("both") {
      benchmark({
        blockingSanity()
      }, "blocking")
      benchmark({
        nonBlockingSanity()
      }, "non-blocking")
      benchmark({
        blockingSanity()
      }, "blocking")
      benchmark({
        nonBlockingSanity()
      }, "non-blocking")
    }
  }

  describe ("Checking with Response") {
    it("both") {
      benchmark({
        blockingSanityResponse()
      }, "blocking")
      benchmark({
        nonBlockingSanityResponse()
      }, "non-blocking")
      benchmark({
        blockingSanityResponse()
      }, "blocking")
      benchmark({
        nonBlockingSanityResponse()
      }, "non-blocking")
      benchmark({
        blockingSanityResponse()
      }, "blocking")
      benchmark({
        nonBlockingSanityResponse()
      }, "non-blocking")
      benchmark({
        blockingSanityResponse()
      }, "blocking")
      benchmark({
        nonBlockingSanityResponse()
      }, "non-blocking")
      benchmark({
        blockingSanityResponse()
      }, "blocking")
      benchmark({
        nonBlockingSanityResponse()
      }, "non-blocking")
    }
  }

  describe ("Testing Multi Blocking") {
    it("be ok please") {
      TardHttpClient.mBlockingRequests(Seq(req1, req1)) shouldBe 0
    }
  }
}