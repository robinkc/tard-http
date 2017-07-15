package in.kcrob.tardhttp

import rx.lang.scala
import rx.lang.scala.JavaConversions._
/**
  * Created by kcrob.in on 15/07/17.
  */
class HystrixCommandTest extends UnitSpec{
  describe ("HelloWorld") {
    it("should print hello world") {
      val result = new HelloWorldCommand("world").execute()
      result shouldBe "Hello world!"
    }

    it("should print hello world observable") {
      val command: scala.Observable[String] = new HelloWorldObservableCommand("world").toObservable()
      command.subscribe (
        valu => {
          println (valu)
        },
        e => {
          println ("error")
          e.printStackTrace()
        },
        () => {
          println("done")
        }
      )

//      command.observe().subscribe( {
//        value => print value
//      })
//      result shouldBe "Hello world!"
    }
  }

}
