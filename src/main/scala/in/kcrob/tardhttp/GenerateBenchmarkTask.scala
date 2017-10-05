package in.kcrob.tardhttp

import java.io.File

import org.openjdk.jmh.Main
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.Options
import org.openjdk.jmh.runner.options.OptionsBuilder


/**
  * Created by kcrob.in on 27/09/17.
  */
object GenerateBenchmarkTask extends App{
  new Abc()
}

class Abc {
  println("Starting..")

  private val generatedDestination = "src/main/scala"

  println("Deleting previous setup..")

  val index = new File(generatedDestination + "/in/kcrob/tardhttp/jmh/generated")
  val entries: Array[String] = if(index.list == null ){
    Array()
  }
  else{
    index.list
  }
  for (s <- entries) {
    val currentFile = new File(index.getPath, s)
    currentFile.delete()
  }

  new File("src/main/resources/META-INF/BenchmarkList").delete()

  JmhBytecodeGenerator.main(Array("out/production/classes", generatedDestination, "src/main/resources"))

//  Main.main(Array("-jvmArgsAppend", "-cp /Users/robinchugh/IdeaProjects/tard-http/out/production/classes"))
//  Main.main(Array("-h"))
//  Main.main(Array())
  //  val opt: Options = new OptionsBuilder().include(classOf[HelloWorld].getSimpleName).forks(1).build
  //  new Runner(opt).run()
}