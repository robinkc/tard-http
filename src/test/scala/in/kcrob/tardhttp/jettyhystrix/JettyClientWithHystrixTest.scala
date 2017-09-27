package in.kcrob.tardhttp.jettyhystrix

import java.util.concurrent.CountDownLatch

import in.kcrob.tardhttp.HttpHystrixObservableCommand
import in.kcrob.tardhttp.jetty.JettyClientTestSpec
import rx.lang.scala
import rx.lang.scala.JavaConversions._

/**
  * Created by kcrob.in on 15/09/17.
  */
class JettyClientWithHystrixTest
  extends JettyClientTestSpec{

  describe("Jetty and Hystrix together") {
    it("Should work fine for multiple calls") {
      val n = 2

      val latch = new CountDownLatch(n)
      var response1: String = ""
      var response2: String = ""

      val command: scala.Observable[String] = new HttpHystrixObservableCommand(req1).observe()

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

      val command2: scala.Observable[String] = new HttpHystrixObservableCommand(req2).toObservable

      command2.subscribe(
        next => {
          response2 = next
        },
        error => {
          latch.countDown()
        },
        () => {
          latch.countDown()
        }
      )

      //TODO::Catch Exception and abort all requests.
      latch.await()

      response1 shouldBe "Cool story, bro. - robin"
      response2 shouldBe "Bravo mike, robin. - chugh"
    }
  }

}
