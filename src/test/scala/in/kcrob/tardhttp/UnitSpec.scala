package in.kcrob.tardhttp

import in.kcrob.scalacommon.Logging
import org.scalatest._

/**
  * Created by kcrob.in on 15/07/17.
  */
abstract class UnitSpec
  extends FunSpec
    with Matchers
    with OptionValues
    with Inside
    with Inspectors
    with BeforeAndAfter
    with Logging
