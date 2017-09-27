# tard-http
A resilient performant http client

We want to provide 

1. Blocking client for both get and mget
2. Configure both Hystrix and Jetty from one TardHttpConfig
3. Tardhttp.stop shall stop both jetty and hystrix
4. Jmx metrics


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

TODOs
1. Done - Checked in io.reactivex.netty.protocol.http.client.HttpClientImpl Observable.create(new Observable.OnSubscribe<HttpClientResponse<O>>) is used so we are good.
2. Done - Lets find how do errors on observables work. 
3. Done: Use Jetty Timeouts and don't use Hystrix Timeouts - Based on below we will rely on Jetty Timeouts, which are configured per request.
    1. command.timeout.enabled will disable hystrix timeouts, so we will simply say observable.onError
    2. Jetty send() is a blocking call, it catches interrupted exception and calls request.abort. 
    Internally jetty is using a TimeoutResponseListener which is scheduled on ScheduledExecutorScheduler which 
     runs the task when after a delay.
         1. If response completes before timeout, scheduled task is cancelled. 
            This way we never had to use another thread.
         2. If response does not complete on time, scheduled task is run and that aborts the request.
            This way, all the response listeners are called with failure.
         3. So calling a request.abort allows us to notify all response listeners to stop listening.
             That is why we will catch exception and abort all requests.
     3. Jetty has a complete timeout, that can be passed while calling send.
         1. This means extra processing by the way.
         2. Try avoiding it as it happens at every request.
     4. Or we can set specific timeouts on httpClient.
         1. This is granular - specific connect timeout, read timeout etc
     5. Done: Check if Hsytrix disable timeout works
         1. Hystrix Timeout works in almost the same way as Jetty Timeout - schedule a timeout-task, and if command succeeds, cancel it.
         2. If timeout is disabled, Hystrix skips all this processing. Check in AbstractCommand.executeCommandAndObserve
4. Done: How to unit test fallbacks of Hystrix command
    1. We can either use hystrix.command.default.circuitBreaker.forceOpen
    2. OR Set metrics.healthSnapshot.intervalInMilliseconds = 10 and circuitBreaker.requestVolumeThreshold = 1 so after 10 milliseconds when a snapshot is taken, we will start seeing failure.
5. Done: Does it make any sense for hystrixobservablecommand to have isolation = thread?
    1. The default, and the recommended setting, is to run HystrixCommands using thread isolation (THREAD) and HystrixObservableCommands using semaphore isolation (SEMAPHORE)
    2. And it does not make sense to do thread isolation for ObservableCommands
        1. Ref - https://github.com/Netflix/Hystrix/wiki/FAQ%20:%20Operational
6. Done:: How to unit test
    1. Run tests in parallel - http://mrhaki.blogspot.in/2010/11/gradle-goodness-running-tests-in.html
7. Done:: Confirm how many threads Jetty is using
    1. Like this - https://stackoverflow.com/questions/44731317/how-does-jetty-httpclient-use-threads
7. Done:: Concurrency Limit - At any given moment there shall not be more than 100 requests pending on a destination.
    1. It is not a rate limit - You can not say 100 per second.
    2. We need to test in terms of HystrixObservable how does semaphore isolation work, because one observable can return multiple results.
        So, possibly, Hystrix might limit number of non-complete observables.
        .withExecutionIsolationSemaphoreMaxConcurrentRequests works and sets the concurrency to that. It works on commandKey. Please check HystrixCommandTest."semaphore isolation with maximum number of requests"
    3. We should also check if HttpClient.setMaxRequestsQueuedPerDestination's overhead can be completely ignored.
        By default -     private volatile int maxConnectionsPerDestination = 64;
                         private volatile int maxRequestsQueuedPerDestination = 1024;
        maxRequestsQueuedPerDestination is the size of the blocking queue, so, we can not avoid it's performance penalty, if any.
        maxConnectionsPerDestination is the size of PoolingDuplexConnection, we can not avoid it's performance penalty, if any 

7. TODO:: How to pass multiple responses back to the caller thread
8. TODO:: Benchmark and say it is better or worse to use non-blocking io
8. TODO:: Http2 transport
    1. Connection Pools
    2. How many requests over one connection?
9. TODO:: Https
10. TODO:: Decide on how will you receive configuration
    1. Setter configuration of Hystrix
        1. Will there be just one HttpCommand? Or you have to write a new command per API integration
        2. 
    2. Timeout configuration of Jetty
        1. Will there be separate configuration for every request? 
        2. How will we configure different configuration for different servers.
####Notes
1. HystrixObservableCommand.observe (Hot) VS HystrixObservableCommand.toObservable (cold) . Observe internally calls toObservable and creates and extra subscription. We do not need that extra layer.
2. All exceptions thrown from the run() method except for HystrixBadRequestException count as failures and trigger getFallback() and circuit-breaker logic.
3. Dropwizard uses Jersey Client - Jersey has RX Invoker - But it always does a thread pool.
4. Contrasted with linkedin/rest.li - It does not use hystrix
5. You can cache setters like this - private static final Setter cachedSetter = 
                                             Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"))
                                                 .andCommandKey(HystrixCommandKey.Factory.asKey("HelloWorld"));    
                                     
                                         public CommandHelloWorld(String name) {
                                             super(cachedSetter);
                                             this.name = name;
                                         }
6. Hystrix CommandGroup 
