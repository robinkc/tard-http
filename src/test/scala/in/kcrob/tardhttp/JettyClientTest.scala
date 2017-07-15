package in.kcrob.tardhttp

import org.eclipse.jetty.client.HttpClient;

/**
  * Created by kcrob.in on 15/07/17.
  */
class JettyClientTest extends UnitSpec{
  describe ("Jetty Http Client") {
    it("should work ok") {
      val httpClient = new HttpClient
    }
  }

}
