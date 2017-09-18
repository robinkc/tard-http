package in.kcrob.tardhttp.jetty

import java.util.concurrent.CountDownLatch

import org.eclipse.jetty.client.api.Result
import org.eclipse.jetty.client.util.BufferingResponseListener

/**
  * Created by kcrob.in on 15/07/17.
  */

class JettyClientTest
  extends JettyTestSpec{
  describe ("Jetty Http Client should work ok") {
    it("in sync mode") {

      val response = httpClient.newRequest("http://foaas.com/cool/robin")
        .header("Accept", "text/plain")
        .send()

      response.getStatus shouldBe 200
      response.getContentAsString shouldBe "Cool story, bro. - robin"

    }

    it("in async mode") {
      val latch = new CountDownLatch(2)
      var response1: String = ""
      var response2: String = ""
      req1.send(new BufferingResponseListener() {
          override def onComplete(result: Result): Unit = {
            response1 = getContentAsString
            latch.countDown()
          }
        })

      req2.send(new BufferingResponseListener() {
          override def onComplete(result: Result): Unit = {
            response2 = getContentAsString
            latch.countDown()
          }
        })

      latch.await()

      response1 shouldBe "Cool story, bro. - robin"
      response2 shouldBe "Bravo mike, robin.- chugh"
    }

  }
}