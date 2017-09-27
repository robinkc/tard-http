package in.kcrob.tardhttp

import org.openjdk.jmh.Main

/**
  * Created by kcrob.in on 27/09/17.
  */
object RunBenchmarkTask extends App{
  new RunBenchmarkTask0()
}

class RunBenchmarkTask0 {
  Main.main(Array("-f", "1"))
}
