package in.kcrob.tardhttp.jmh

import java.util.concurrent.{ExecutorService, Executors}

import in.kcrob.scalacommon.Logging
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

/**
  * Created by kcrob.in on 03/10/17.
  */
object HttpComponentsHttpClientSanity extends App{
  new HttpComponentsHttpClientSanity0
}

class HttpComponentsHttpClientSanity0 extends Logging {
  val cm = new PoolingHttpClientConnectionManager
  cm.setMaxTotal(100)
  cm.setDefaultMaxPerRoute(200)
  val httpComponentsHttpClient: CloseableHttpClient = HttpClients
    .custom
    .setConnectionManager(cm)
    .build
//  val httpComponentsHttpClient: CloseableHttpClient = HttpClients.createDefault()

  def httpComponentsRequest: HttpGet = new HttpGet("http://localhost:8080/hello_1000")

  val N_PARALLEL_REQ = 10
  val threadPool: ExecutorService = Executors.newFixedThreadPool(N_PARALLEL_REQ * 6)

  run()

  def run() = {
    LOG.info("Inside Run")

    for( i <- 0 until N_PARALLEL_REQ) {
      threadPool.submit(new Runnable {
        override def run(): Unit = {
          LOG.debug("Inside Runnable.run")

          var contentResponse: CloseableHttpResponse = null
          try {
            LOG.debug("Initiating request")
            contentResponse = httpComponentsHttpClient.execute(httpComponentsRequest)
            LOG.debug("Got response")
            val str = IOUtils.toString(contentResponse.getEntity.getContent)
            str
          }
          finally {
            contentResponse.close()
          }

        }
      })
    }

  }
}