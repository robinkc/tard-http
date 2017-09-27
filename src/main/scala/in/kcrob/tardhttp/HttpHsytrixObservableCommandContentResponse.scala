/**
  * Created by kcrob.in on 14/08/17.
  */

package in.kcrob.tardhttp

import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey, HystrixCommandProperties, HystrixObservableCommand}
import in.kcrob.scalacommon.Logging
import org.eclipse.jetty.client.api.{Request, Response, Result}
import org.eclipse.jetty.client.util.BufferingResponseListener
import rx.{Observable, Subscriber}

/**
  * Created by kcrob.in on 14/08/17.
  */
class HttpHystrixCommandContentResponse (val req: Request)
  extends HystrixCommand[Response](
    Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionTimeoutInMilliseconds(5000)
      )
  )
    with Logging {
  override def run(): Response = {
    req.send()
  }
}

class HttpHsytrixObservableCommandContentResponse (val req: Request)
  extends HystrixObservableCommand[Response] (
    HystrixObservableCommand.Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionTimeoutInMilliseconds(5000)
      )
  )
    with Logging {

  override def construct(): Observable[Response] = {
    Observable.create(new Observable.OnSubscribe[Response] {

      override def call(t: Subscriber[_ >: Response]): Unit = {
        req.send(new BufferingResponseListener() {
          override def onComplete(result: Result): Unit = {
            if(result.isFailed) {
              LOG.error("Request Failed")
              t.onError(result.getFailure)
            }
            else {
              LOG.debug("Calling onNext")
              t.onNext(result.getResponse) //Running in Jetty Thread
              LOG.debug("Calling onCompleted")
              t.onCompleted()
            }
          }
        })
      }
    })
  }
}
