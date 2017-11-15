package in.kcrob.tardhttp.jmh

import in.kcrob.scalacommon.Logging

/**
  * Created by kcrob.in on 27/09/17.
  */
object MyBenchmarkSanity
  extends App
  with Logging{
  new MyBenchmarkSanity0
}

class MyBenchmarkSanity0
  extends Logging{
  private val jettyClientBenchmark = new JettyClientBenchmark
  jettyClientBenchmark.benchJettyClient()
  jettyClientBenchmark.tearDown()

  private val httpComponentsBenchmark = new HttpComponentsBenchmark
  httpComponentsBenchmark.benchHystrixBlockingCommandHttpComponents()
  httpComponentsBenchmark.tearDown()

  LOG.info("We are done")
}