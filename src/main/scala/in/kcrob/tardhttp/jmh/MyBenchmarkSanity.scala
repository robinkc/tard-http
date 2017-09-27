package in.kcrob.tardhttp.jmh

/**
  * Created by kcrob.in on 27/09/17.
  */
object MyBenchmarkSanity extends App{
  new MyBenchmarkSanity0
}

class MyBenchmarkSanity0 {
  new MyBenchmarkOnlyMine().benchHttpHystrixObservableCommand
}