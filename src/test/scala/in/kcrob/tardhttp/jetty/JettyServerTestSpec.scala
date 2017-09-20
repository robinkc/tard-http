package in.kcrob.tardhttp.jetty

import java.io.ByteArrayInputStream
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import in.kcrob.scalacommon.Logging
import in.kcrob.tardhttp.UnitSpec
import org.eclipse.jetty.server.{Handler, Request, Server, ServerConnector}
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.http.HttpScheme
import org.eclipse.jetty.server.handler.AbstractHandler
import org.apache.commons.io.IOUtils
/**
  * Created by kcrob.in on 19/09/17.
  */
//Inspired by http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/jetty-client/src/test/java/org/eclipse/jetty/client/AbstractHttpClientServerTest.java
//TODO:: Need to add ability to keep one or more servers up and running, while multiple tests
//TODO::  can share a server if they want
//TODO::  can run tests in parallel
//TODO::  servers are shutdown after all referring test cases have completed
//TODO::  Make it a trait so that one can add this functionality if needed
//TODO::  Also, possible to add httpClient in the trait separately

class JettyServerTestSpec (handler: Handler)
  extends UnitSpec
    with Logging{
  private val scheme: String = HttpScheme.HTTP.asString()
  protected val server: Server = {
    val serverThreads = new QueuedThreadPool
    serverThreads.setName("server")
    val s = new Server(serverThreads)
    val ssl: SslContextFactory = null
    val connector : ServerConnector = new ServerConnector(s, ssl)
    s.addConnector(connector)
    s.setHandler(handler)
    s.start()
    s
  }
  protected val port: Int = server.getConnectors()(0).asInstanceOf[ServerConnector].getLocalPort
  protected val baseUrl: String = s"$scheme://localhost:$port/"

  LOG.info(s"Server started with baseUrl = $baseUrl ")
}

//What is an handler - http://www.eclipse.org/jetty/documentation/9.4.x/jetty-handlers.html
//Inspired by http://git.eclipse.org/c/jetty/org.eclipse.jetty.project.git/tree/jetty-client/src/test/java/org/eclipse/jetty/client/RespondThenConsumeHandler.java
class RequestHandler extends AbstractHandler {
  override def handle(target: String, baseRequest: Request, request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val x = target match {
      case "/hello" =>
        baseRequest.setHandled(true)
        response.setStatus(200)
        IOUtils.copy(new ByteArrayInputStream( "world".getBytes()), response.getOutputStream)
        response.flushBuffer()

      case "/body" =>
        baseRequest.setHandled(true)
        response.setStatus(200)
        IOUtils.copy(request.getInputStream, response.getOutputStream)
        response.flushBuffer()
    }
  }
}