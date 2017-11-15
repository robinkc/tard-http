package in.kcrob.tardhttp.jmh

import java.util.concurrent.{ConcurrentHashMap, CountDownLatch}

import com.netflix.hystrix.HystrixObservableCommand
import rx.lang.scala
import rx.lang.scala.JavaConversions._

/**
  * Created by kcrob.in on 09/10/17.
  */
abstract class BaseHystrixObservableCommandBenchmark
  extends BaseTardHttpBenchmark{

  protected def benchHystrixObservableCommandWithConcurrentHashMap(commandBuilder: () => HystrixObservableCommand[String]): java.util.Map[Int, String] = {

    //    println ("Testing observable command")
    //Using ConcurrentHashMap even for a single request, as not expecting any contention, no contention no performance hit
    val map: java.util.Map[Int, String] = new ConcurrentHashMap[Int, String]()

    var response1: String = null

    val latch = new CountDownLatch(N_PARALLEL_REQ)

    for (i <- 0 until N_PARALLEL_REQ) {
      val observable: scala.Observable[String] = commandBuilder().toObservable()

      observable.subscribe(
        next => {
          map.put(i, next)
        },
        error => {
          latch.countDown()
          handleException(error, map, i)
        },
        () => {
          latch.countDown()
        }
      )
    }

    latch.await()

    //    verifyResults(map)

    map

  }

}
