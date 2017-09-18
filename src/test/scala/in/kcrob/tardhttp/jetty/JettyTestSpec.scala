package in.kcrob.tardhttp.jetty

import in.kcrob.tardhttp.UnitSpec
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
  * Created by kcrob.in on 15/09/17.
  */
class JettyTestSpec extends UnitSpec{
  var httpClient: HttpClient = _

  before {
    httpClient = new HttpClient(new SslContextFactory())
    httpClient.setFollowRedirects(false)
    httpClient.start()
  }

  after {
    httpClient.stop()
  }

  def req1: Request = httpClient.newRequest("http://foaas.com/cool/robin")
    .header("Accept", "text/plain")

  def req2 = httpClient.newRequest("http://foaas.com/bm/robin/chugh")
    .header("Accept", "text/plain")

}

