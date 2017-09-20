package in.kcrob.tardhttp.basic

import com.netflix.hystrix.HystrixCommand.Setter
import com.netflix.hystrix.{HystrixCommand, HystrixCommandGroupKey, HystrixCommandProperties, HystrixObservableCommand}
import in.kcrob.scalacommon.Logging
import org.slf4j.LoggerFactory
import rx.{Observable, Subscriber}

/**
  * Created by kcrob.in on 15/07/17.
  */

class HelloWorldCommand (val name: String)
  extends HystrixCommand[String](HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
    with Logging {
  override protected def run: String = {
    LOG.info("Running the command.")
    "Hello " + name + "!"
  }
}

class HelloWorldCommandWithTimeout(name: String)
  extends HystrixCommand[String](
    Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionTimeoutInMilliseconds(50)
      )
  ) with Logging {
  override protected def run: String = {
    LOG.info("Running the command.")
    Thread.sleep(100)
    "Hello " + name + "!"
  }

  override def getFallback: String = "Sorry " + name + "!"
}

class HelloWorldCommandWithDisabledTimeout(name: String)
  extends HystrixCommand[String](
    Setter
      .withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
      .andCommandPropertiesDefaults(
        HystrixCommandProperties
          .Setter()
          .withExecutionTimeoutInMilliseconds(50)
          .withExecutionTimeoutEnabled(false)
      )
  ) with Logging {
  override protected def run: String = {
    LOG.info("Running the command.")
    Thread.sleep(100)
    "Hello " + name + "!"
  }

  override def getFallback: String = "Sorry " + name + "!"
}

class HelloWorldObservableCommand(val name: String, index: Int) extends HystrixObservableCommand[String] (
  HystrixObservableCommand.Setter
    .withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
    .andCommandPropertiesDefaults(
      HystrixCommandProperties
        .Setter()
        .withExecutionTimeoutInMilliseconds(5000)
    )
) {
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