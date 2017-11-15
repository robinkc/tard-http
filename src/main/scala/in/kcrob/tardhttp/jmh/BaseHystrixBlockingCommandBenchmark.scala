package in.kcrob.tardhttp.jmh

import java.util
import java.util.concurrent.{ConcurrentHashMap, CountDownLatch, ExecutorService, Executors}

import com.netflix.hystrix.HystrixCommand
import org.openjdk.jmh.annotations.TearDown

/**
  * Created by kcrob.in on 09/10/17.
  */
abstract class BaseHystrixBlockingCommandBenchmark
extends BaseTardHttpBenchmark{

  //Extra Setup
  val threadPool: ExecutorService = Executors.newFixedThreadPool(N_CONCURRENCY)

  protected def benchHystrixBlockingCommand(commandBuilder :() => HystrixCommand[String]): java.util.Map[Int, String] = {

    val map: java.util.Map[Int, String] =     if(N_PARALLEL_REQ == 1) {
      new util.HashMap[Int, String]()
    }
    else{
      new ConcurrentHashMap[Int, String]()
    }

    //We will not create a thread if it is one request
    if(N_PARALLEL_REQ == 1) {
      try {
        map.put(0, commandBuilder().execute())
      }
      catch {
        case error: Throwable => handleException(error, map, 0)
      }
    }

    else {
      val latch = new CountDownLatch(N_PARALLEL_REQ)

      for (i <- 0 until N_PARALLEL_REQ) {
        val command = commandBuilder()
        threadPool.submit(new Runnable {
          override def run(): Unit = {
            try {
              val next: String = command.execute()
              map.put(i, next)
            }
            catch {
              case error: Throwable => handleException(error, map, i)
            }
            finally {
              latch.countDown()
            }
          }
        })
      }

      //      latch.await(TIMEOUT, TimeUnit.MILLISECONDS)
      latch.await()
    }

    //    verifyResults(map)
    map

  }

  override def tearDown(): Unit = {
    threadPool.shutdown()
    super.tearDown()
  }

}
