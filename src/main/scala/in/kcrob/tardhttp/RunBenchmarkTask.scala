package in.kcrob.tardhttp

import in.kcrob.tardhttp.RunBenchmarkTask.args
import org.openjdk.jmh.Main

/**
  * Created by kcrob.in on 27/09/17.
  */
object RunBenchmarkTask extends App{
  override def main(args: Array[String]): Unit = {
    super.main(args)
    new RunBenchmarkTask0(args)
  }
}

class RunBenchmarkTask0(args: Array[String]) {
  println(System.getProperty("SERVER_BASE_URL"))
  val N_THREADS: String = System.getProperty("N_THREADS")

  private val newArgs: Array[String] = args ++ Array("-t", N_THREADS)

  Main.main(newArgs)
//  Main.main(Array("-h"))
}
