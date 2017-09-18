package in.kcrob.scalacommon

import org.slf4j.LoggerFactory

/**
  * Created by kcrob.in on 14/08/17.
  */
trait Logging {
  private val _LOG = LoggerFactory.getLogger(getClass)
  def LOG = _LOG
}
