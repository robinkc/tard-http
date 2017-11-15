package com.cleartrip.clearhttp.api


/**
  * Created by kcrob.in on 11/10/17.
  */
trait HttpClient {
  def execute(request: HttpRequest): HttpResponse
}

trait HttpResponse {
  def status: Int
  def content: Array[Byte]
}

trait HttpRequest {
  def url: String
  def body: Array[Byte]
  def api: HttpApi
}

case class HttpApiConf(
  url: String,
  timeout: Int,
  maxConcurrency: Int
)

trait HystrixSetter

abstract class HttpApi(conf: HttpApiConf) {
  def newRequest: HttpRequest
  def hystrixSetter: HystrixSetter
}

case class HttpRequestImpl(url: String, body: Array[Byte], api: HttpApi) extends HttpRequest
case class HttpResponseImpl(status: Int, content: Array[Byte]) extends HttpResponse

class HttpClientImpl extends HttpClient {
  override def execute(request: HttpRequest): HttpResponse = {
    new ExecuteHttpRequestCommand(request, request.api.hystrixSetter).exeute()
  }
}

class ExecuteHttpRequestCommand (request: HttpRequest, setter: HystrixSetter) {
  def exeute(): HttpResponse = {
    HttpResponseImpl(200, "Hello World".getBytes)
  }
}

object Test {

}