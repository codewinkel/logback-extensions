#logback-extensions

##About

The `logback-extensions` modul as an add-on for your logback context.
This module does the following things:

* `DefaultHttpGetAppender`: the DefaultHttpGetAppender can send some GET requests to an configured requestUrl.
* `HockeyappCrashAppender`: the HockeyappCrashAppender can send the stack trace of the exception of your program to hockeyapp, so hockey app can visualize your *exception flows* and create tickets in your bug tracker system

##Maven

Current version is available at central repository

    <dependencies>
        ...
        <dependency>
	        <groupId>com.mikewinkelmann.logging</groupId>
		    <artifactId>logback-extensions</artifactId>
		    <version>0.0.1-SNAPSHOT</version>
	    </dependency>
        ...
    </dependencies>

##Configuration

###DefaultHttpGetAppender
This appender creates and executes a http GET request against the configured requestUrl.
####Properties
Some properties are required some are optional

* `requestUrl` required, request url for the HTTP GET request
* `logState` optional, every log state you want to execute a GET call. Available states are the known: ERROR, WARN, INFO, DEBUG, TRACE you can also configure more than one log state with more logState tags - default: nothing
* `successStatusCodeMin` optional, minimum successful http status code - default: 200
* `successStatusCodeMax` optional, maximum successful http status code - default: 299
* `queueSize` optional, size of the executing reporting queue - default: 0

####Example  

    <appender name="GETAPPENDER" class="com.mwinkelmann.logging.appender.http.DefaulthttpGetAppender">
		<requestUrl>http://localhost:8080/testservice</requestUrl>
		<logState>ERROR</logState>
		<logState>WARN</logState>
		<logState>INFO</logState>
		<logState>DEBUG</logState>
		<logState>TRACE</logState>
		<successStatusCodeMin>200</successStatusCodeMin>
		<successStatusCodeMax>299</successStatusCodeMax>
		<queueSize>0</queueSize>
	</appender>

Work in progress :)