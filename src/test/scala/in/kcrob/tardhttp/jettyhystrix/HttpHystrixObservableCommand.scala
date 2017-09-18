package in.kcrob.tardhttp.jettyhystrix

import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey, HystrixCommandProperties, HystrixObservableCommand}
import in.kcrob.scalacommon.Logging
import org.eclipse.jetty.client.api.{ContentResponse, Request, Result}
import org.eclipse.jetty.client.util.BufferingResponseListener
import rx.{Observable, Subscriber}

/**
  * Created by kcrob.in on 14/08/17.
  */
class HttpHystrixCommand (val req: Request)
  extends HystrixCommand[String](
    Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionTimeoutInMilliseconds(5000)
      )
  )
  with Logging {
  override def run(): String = {
    val contentResponse: ContentResponse = req.send()
    contentResponse.getContentAsString //Running in Hsytrix Thread
  }
}

class HttpHystrixObservableCommand (val req: Request)
  extends HystrixObservableCommand[String] (
    HystrixObservableCommand.Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionTimeoutInMilliseconds(5000)
      )
  )
  with Logging {

  override def construct(): Observable[String] = {
    Observable.create(new Observable.OnSubscribe[String] {

      override def call(t: Subscriber[_ >: String]): Unit = {
        req.send(new BufferingResponseListener() {
          override def onComplete(result: Result): Unit = {
            if(result.isFailed) {
              LOG.error("Request Failed")
              t.onError(result.getFailure)
            }
            else {
              LOG.debug("Calling onNext")
              t.onNext(getContentAsString) //Running in Jetty Thread
              LOG.debug("Calling onCompleted")
              t.onCompleted()
            }
          }
        })
      }
    })
  }
}