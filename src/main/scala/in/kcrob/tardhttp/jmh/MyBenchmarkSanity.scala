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
  private val tardHttpBenchmark = new TardHttpBenchmark()
  tardHttpBenchmark.benchTardHttpClientWithConcurrentHashMap()
  tardHttpBenchmark.benchHttpComponentsHttpClient()
  tardHttpBenchmark.tearDown()

  LOG.info("We are done")
}