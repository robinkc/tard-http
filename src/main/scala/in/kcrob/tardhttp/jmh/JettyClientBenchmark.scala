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
  * Created by kcrob.in on 09/10/17.
  */
@State(Scope.Benchmark)
class JettyClientBenchmark
  extends BaseHystrixObservableCommandBenchmark{

  //Jetty Client Setup
  val jettyHttpClient = new HttpClient(new SslContextFactory())
  jettyHttpClient.setFollowRedirects(false)
  jettyHttpClient.setMaxConnectionsPerDestination(N_CONCURRENCY)
  jettyHttpClient.start()


  def jettyRequest1: Request = jettyHttpClient.newRequest(requestUrl1)

  @TearDown
  override def tearDown(): Unit = {
    jettyHttpClient.stop()
    super.tearDown()
  }

  override def setup(): Unit = {
    super.setup()
  }

  protected def getHystrixCommand(req: Request): HystrixCommand[String] = {
    new HttpHystrixCommand(req, TIMEOUT, N_CONCURRENCY)
  }

  val hystrixJettyBlockingCommandBuilder: () => HystrixCommand[String] = () => {
    getHystrixCommand(jettyRequest1)
  }

  protected def getHystrixObservableCommand(req: Request): HystrixObservableCommand[String] = {
    new HttpHystrixObservableCommand(req, TIMEOUT, N_CONCURRENCY)
  }

  val hystrixObservableCommandBuilder: () => HystrixObservableCommand[String] = () => {
    getHystrixObservableCommand(jettyRequest1)
  }

  @Benchmark
  def benchJettyClient(): java.util.Map[Int, String] = {
    benchHystrixObservableCommandWithConcurrentHashMap(hystrixObservableCommandBuilder)
  }

  override def setup0(): Unit = {
    benchJettyClient()
  }
}
