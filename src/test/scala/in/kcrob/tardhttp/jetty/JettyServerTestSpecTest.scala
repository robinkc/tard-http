package in.kcrob.tardhttp.jetty

import in.kcrob.tardhttp.UnitSpec
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.client.util.StringContentProvider
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
  * Created by kcrob.in on 19/09/17.
  */
class JettyServerTestSpecTest
  extends JettyServerTestSpec(new RequestHandler){

  var httpClient: HttpClient = _

  before {
    httpClient = new HttpClient(new SslContextFactory())
    httpClient.setFollowRedirects(false)
    httpClient.start()
  }

  after {
    httpClient.stop()
  }

  describe("JettyServerTestSpec") {
    it("/hello returns world") {
      val response = httpClient.newRequest(s"${baseUrl}hello").send()
      response.getContentAsString shouldBe "world"
    }
    it("/body responds what is sent") {
      val response = httpClient.newRequest(s"${baseUrl}body")
        .content(new StringContentProvider("world"))
        .send()
      response.getContentAsString shouldBe "world"
    }
  }
}
