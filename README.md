# tard-http
A resilient performant http client

We want to provide 

1. Blocking client for both get and mget
    1. for get - jetty client has Request.send() which creates FutureResponseListener and uses latch.countdown
    2. We need to do for mget, something similar
2. Be able to use hystrix
3. Internally use something non-blocking only.
4. Possibily providing http2
5. Also provides https


Order of steps
1. Benchmark - Single call blocking, non-blocking
2. Benchmark - two calls blocking, non-blocking
3. Metrics and Dashboards
4. Error And circuit breaking



things yet to do
1. Semaphore isolation
2. Http2 transport in jetty client
3. Bulk http requests in jetty client
    1. Need to isolate individual call within multiple calls
4. Connection Pooling
5. Stop httpClient
6. CountdownLatch + ConcurrentHashMap VS Observables.zip


Looking at SingleBenchmark
Blocking shall perform worse than non-blocking because
1. HystrixCommand.execute will call run method in hystrix-ExampleGroup-1 thread
2. Which will call FutureResponseListener.get which in turn will wait for HttpClient@45495894-16 thread to call FutureResponseListener.latch.countDown
3. So response will be passed from HttpClient@9434034-16 thread to hystrix-ExampleGroup-1 thread
4. Which will pass the response to scalatest-run thread
5. Response is org.eclipse.jetty.client.HttpContentResponse which has all private final methods


Non Blocking shall perform better because 
1. We will not have anything on hystrix-ExampleGroup-1 thread

--------------

1. Checked in io.reactivex.netty.protocol.http.client.HttpClientImpl Observable.create(new Observable.OnSubscribe<HttpClientResponse<O>>) is used sp we are good.
2. Lets find how do errors on observables work.
3. HystrixObservableCommand.observe (Hot) VS HystrixObservableCommand.toObservable (cold) . Observe internally calls toObservable and creates and extra subscription. We do not need that extra layer.
4. All exceptions thrown from the run() method except for HystrixBadRequestException count as failures and trigger getFallback() and circuit-breaker logic.
5. Use Jetty Timeouts and don't use Hystrix Timeouts
