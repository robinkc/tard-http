package in.kcrob.tardhttp

import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey, HystrixObservableCommand}
import org.slf4j.LoggerFactory
import rx.{Observable, Subscriber}

/**
  * Created by kcrob.in on 15/07/17.
  */

class HelloWorldCommand (val name: String) extends HystrixCommand[String](HystrixCommandGroupKey.Factory.asKey("ExampleGroup")){
  private val LOG = LoggerFactory.getLogger(getClass)

  override protected def run: String = {
    LOG.error("Running the command.")
    "Hello " + name + "!"
  }

}

class HelloWorldObservableCommand(val name: String) extends HystrixObservableCommand[String] (HystrixCommandGroupKey.Factory.asKey("ExampleGroup")) {
  private val LOG = LoggerFactory.getLogger(getClass)
  override def construct(): Observable[String] = {
    Observable.create(new Observable.OnSubscribe[String] {
      override def call(t: Subscriber[_ >: String]): Unit = {
        LOG.info("Running observable command")
        s"Hello $name! observed"
      }
    })
  }
}