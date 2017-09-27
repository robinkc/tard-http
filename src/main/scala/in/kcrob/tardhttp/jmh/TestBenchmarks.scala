package in.kcrob.tardhttp.jmh

import in.kcrob.tardhttp.jetty.{EmbeddedJettyServer, RequestHandler}
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.openjdk.jmh.annotations.{Benchmark, Scope, State, TearDown}

/**
  * Created by kcrob.in on 27/09/17.
  */

@State(Scope.Benchmark)
class TestBenchmarks {
  val httpClient = new HttpClient(new SslContextFactory())
  httpClient.setFollowRedirects(false)
  httpClient.start()

  val server = new EmbeddedJettyServer(new RequestHandler)

  val baseUrl: String = server.baseUrl

  def req1: Request = httpClient.newRequest(s"$baseUrl/hello")

//  @Benchmark
  def helloWorld(): String = {
    httpClient.newRequest(s"${baseUrl}hello").send().getContentAsString
  }

  @TearDown
  def stopHttpServer(): Unit = {
    println("Stopping http server")
    server.stop()
    httpClient.stop()
  }
}