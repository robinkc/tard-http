package in.kcrob.tardhttp.basic

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

class HelloWorldObservableCommand(val name: String, index: Int) extends HystrixObservableCommand[String] (HystrixCommandGroupKey.Factory.asKey("ExampleGroup")) {
  private val LOG = LoggerFactory.getLogger(getClass)
  override def construct(): Observable[String] = {
    LOG.info(s"@${index}Constructing a new Observable")

    Observable.create(new Observable.OnSubscribe[String] {

      override def call(t: Subscriber[_ >: String]): Unit = {
        if(name == "DIE") {
          LOG.info(s"@${index} Running observable command - Dying")
          t.onError(new SuicideException)
          LOG.info(s"@${index} Logging after Death")
        }
        else {
          LOG.info(s"@${index} Running observable command")
          LOG.info(s"@${index} Calling onNext")
          t.onNext(s"@${index} Hello $name! observed")
          LOG.info(s"@${index} Calling onCompleted")
          t.onCompleted()
          LOG.info(s"@${index} After onCompleted")
        }
      }
    })
  }

  override def resumeWithFallback(): Observable[String] = {
    Observable.just(s"@${index} Hello from the Night King")
  }
}

class SuicideException extends Exception