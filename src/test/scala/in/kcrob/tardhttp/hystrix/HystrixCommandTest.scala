package in.kcrob.tardhttp.hystrix

import in.kcrob.tardhttp.UnitSpec
import in.kcrob.tardhttp.basic.{HelloWorldCommand, HelloWorldCommandWithDisabledTimeout, HelloWorldCommandWithTimeout, HelloWorldObservableCommand}
import rx.lang.scala
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable
/**
  * Created by kcrob.in on 15/07/17.
  */
class HystrixCommandTest extends UnitSpec{
  describe ("HelloWorld") {
    it("should print hello world") {
      val result = new HelloWorldCommand("world").execute()
      result shouldBe "Hello world!"
    }

    it("throws fallsback with timeout") {
      val result = new HelloWorldCommandWithTimeout("world").execute()
      result shouldBe "Sorry world!"
    }

    it("continues executing even after timeout if it has been disabled") {
      val result = new HelloWorldCommandWithDisabledTimeout("world").execute()
      result shouldBe "Hello world!"
    }

    it("should print hello world observable") {
      LOG.info("Creating new HelloWorldObservableCommand")

      val command: scala.Observable[String] = new HelloWorldObservableCommand("world", 0).toObservable()

      LOG.info("Calling subscribe")
      command.subscribe (
        valu => {
          LOG.info(valu)
        },
        e => {
          LOG.error("Received an error")
          e.printStackTrace()
        },
        () => {
          LOG.info("completed")
        }
      )
    }

    it("Resume With fallback should work") {
      LOG.info("Creating new HelloWorldObservableCommand")

      val command: scala.Observable[String] = new HelloWorldObservableCommand("DIE", 0).toObservable()

      LOG.info("Calling subscribe")

      var result: String = ""
      command.subscribe (
        valu => {
          LOG.info(valu)
        },
        e => {
          LOG.error("Received an error")
          e.printStackTrace()
        },
        () => {
          LOG.info("completed")
        }
      )
    }

    it("Should circuit break") {
      //This test is to be ignored, we need to write a test with circuitBreakerForceOpen = true
      val commands: Seq[Observable[String]] = List.range(1, 30).map(i => {
        val command: Observable[String] = new HelloWorldObservableCommand("DIE", i).toObservable()
        command
      })
      commands.foreach( command => {
        command.subscribe (
          valu => {
            LOG.info(valu)
          },
          e => {
            LOG.error("Received an error")
            e.printStackTrace()
          },
          () => {
            LOG.info("completed")
          }
        )
      })
    }
  }
}