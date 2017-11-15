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

@State(Scope.Benchmark)
class HttpComponentsBenchmark
  extends BaseHystrixBlockingCommandBenchmark{

  //HttpComponentsHttpClientSetup
  val cm = new PoolingHttpClientConnectionManager
  cm.setMaxTotal(N_CONCURRENCY)
  cm.setDefaultMaxPerRoute(N_CONCURRENCY)
  val httpComponentsHttpClient: CloseableHttpClient = HttpClients
    .custom
    .setConnectionManager(cm)
    .build
  def httpComponentsRequest1: HttpGet = new HttpGet(requestUrl1)

  override def setup0(): Unit = {
    benchHystrixBlockingCommandHttpComponents()
  }

  @TearDown
  override def tearDown(): Unit = {
    httpComponentsHttpClient.close()
    super.tearDown()
  }

  protected def getHystrixCommand(request: HttpGet = httpComponentsRequest1): HystrixCommand[String] = {
    new HttpHystrixCommandForHttpComponents(httpComponentsHttpClient, request, TIMEOUT, N_CONCURRENCY)
  }

  val hystrixHttpComponentsBlockingCommandBuilder: () => HystrixCommand[String] = () => {
    getHystrixCommand(httpComponentsRequest1)
  }

  @Benchmark
  def benchHystrixBlockingCommandHttpComponents(): java.util.Map[Int, String] = {
    benchHystrixBlockingCommand(hystrixHttpComponentsBlockingCommandBuilder)
  }

}