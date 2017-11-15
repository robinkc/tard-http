package in.kcrob.tardhttp

import java.util.concurrent.TimeUnit

import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix._
import in.kcrob.scalacommon.Logging
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.impl.client.CloseableHttpClient
import org.eclipse.jetty.client.api.{ContentResponse, Request, Result}
import org.eclipse.jetty.client.util.BufferingResponseListener
import rx.{Observable, Subscriber}

/**
  * Created by kcrob.in on 14/08/17.
  */
class HttpHystrixCommand(val req: Request, val timeout: Int, val concurrency: Int )
  extends {
  } with HystrixCommand[String](
    Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("HttpHystrixCommand"))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionTimeoutInMilliseconds(timeout)
          .withCircuitBreakerEnabled(false)
      ).andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(concurrency * 2))
  )
    with Logging {
  override def run(): String = {
    val contentResponse: ContentResponse = req.send()
    contentResponse.getContentAsString //Running in Hsytrix Thread
  }
}

class HttpHystrixCommandForHttpComponents(val httpComponentsHttpClient: CloseableHttpClient, val httpComponentsRequest: HttpGet, val timeout: Int, val concurrency: Int )
  extends HystrixCommand[String](
    Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("HttpHystrixCommandForHttpComponents"))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionTimeoutInMilliseconds(timeout)
          .withCircuitBreakerEnabled(false)
      ).andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter().withCoreSize(concurrency * 2).withMaximumSize(concurrency * 2).withMaxQueueSize(0))
  )
    with Logging {
  override def run(): String = {
    var contentResponse: CloseableHttpResponse = null
    try {
      contentResponse = httpComponentsHttpClient.execute(httpComponentsRequest)
      val str = IOUtils.toString(contentResponse.getEntity.getContent)
      str
    }
    finally {
      contentResponse.close()
    }
  }
}

class HttpHystrixObservableCommand(val req: Request, val timeout: Int, val concurrency: Int )
  extends HystrixObservableCommand[String](
    HystrixObservableCommand.Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("HttpHystrixObservableCommand" + getClass.getSimpleName))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionIsolationSemaphoreMaxConcurrentRequests(concurrency)
          .withExecutionTimeoutEnabled(false)
          .withCircuitBreakerEnabled(false)
      )
  )
    with Logging {

  override def construct(): Observable[String] = {
    Observable.create(new Observable.OnSubscribe[String] {

      override def call(t: Subscriber[_ >: String]): Unit = {
        req.timeout(timeout, TimeUnit.MILLISECONDS).send(new BufferingResponseListener() {
          override def onComplete(result: Result): Unit = {
            if (result.isFailed) {
              t.onError(result.getFailure)
            }
            else {
              try{
                t.onNext(getContentAsString) //Running in Jetty Thread
                t.onCompleted()
              }
              catch {
                case e: Throwable => t.onError(e)
              }
            }
          }
        })
      }
    })
  }
}