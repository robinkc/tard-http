//:~

package in.kcrob.tardhttp

/**
  * Created by kcrob.in on 15/07/17.
  */
class HelloWorldTest extends UnitSpec{
  describe ("HelloWorld") {
    it ("prints") {
      println("hello world")

      val truth = "prevail"
      truth shouldBe "prevail"
    }
  }

}
