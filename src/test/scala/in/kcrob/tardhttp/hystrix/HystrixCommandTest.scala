package in.kcrob.tardhttp.hystrix

import in.kcrob.tardhttp._
import in.kcrob.tardhttp.basic._
import rx.lang.scala
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable
import rx.schedulers.Schedulers
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

    it("semaphore isolation with maximum number of requests") {
      val scheduler = Schedulers.newThread()

      //The_Only_One will become the command key, so in this case, one of them will fail as Hello From the Night King
      val command1 : Observable[String]= new HelloWorldObservableCommandSingleConcurrency("The_Only_One", 1).toObservable
      val command2 : Observable[String]= new HelloWorldObservableCommandSingleConcurrency("The_Only_One", 2).toObservable

      command1.subscribeOn(scheduler).subscribe(
        valu => {
          LOG.info(s"command 1 got $valu")
        },
        e => {
          LOG.error("command 1 Received an error")
          e.printStackTrace()
        },
        () => {
          LOG.info("command 1 completed")
        }
      )
      Thread.sleep(50) //Lets sleep for 300 ms and see what happens

      command2.subscribeOn(scheduler).subscribe(
        valu => {
          LOG.info(s"command 2 got $valu")
        },
        e => {
          LOG.error("command 2 Received an error")
          e.printStackTrace()
        },
        () => {
          LOG.info("command 2 completed")
        }
      )

      //Expecting command 3 and 4 both to succeed as they will have different command keys
      val command3 : Observable[String]= new HelloWorldObservableCommandSingleConcurrency("Not_The_Only_One", 1).toObservable
      val command4 : Observable[String]= new HelloWorldObservableCommandSingleConcurrency("Certainly Not_The_Only_One", 2).toObservable

      command3.subscribeOn(scheduler).subscribe(
        valu => {
          LOG.info(s"command 3 got $valu")
        },
        e => {
          LOG.error("command 3 Received an error")
          e.printStackTrace()
        },
        () => {
          LOG.info("command 3 completed")
        }
      )

      command4.subscribeOn(scheduler).subscribe(
        valu => {
          LOG.info(s"command 4 got $valu")
        },
        e => {
          LOG.error("command 4 Received an error")
          e.printStackTrace()
        },
        () => {
          LOG.info("command 4 completed")
        }
      )

      Thread.sleep(300) //Lets sleep for 300 ms and see what happens
    }
  }
}