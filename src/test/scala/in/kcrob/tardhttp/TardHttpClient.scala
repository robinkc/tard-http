package in.kcrob.tardhttp

import java.lang

import org.eclipse.jetty.client.api.{Request, Response}
import rx.functions.{Func1, FuncN}
import rx.lang.scala
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

import collection.JavaConversions._

/**
  * Created by kcrob.in on 14/08/17.
  */
object TardHttpClient {

  def mBlockingRequests (reqs: Seq[Request]): Int = {
    val observables: Seq[scala.Observable[Int]] = reqs.map(
      req => {
        val command : scala.Observable[Response] = new HttpHystrixCommandContentResponse(req).toObservable
        command.map(_.getStatus)
      }
    )

    val funcN: FuncN[Int] = new FuncN[Int]() {
      override def call(args: AnyRef*): Int = 0
    }

    val single  = observables.reduce((a, b) => {
      val zipped: Observable[(Int, Int)] = a zip b
      zipped.map(v => v._1 + v._2)
    }).toBlocking.single

    single
  }

}