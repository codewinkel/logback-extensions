#logback-extensions

##About

The `logback-extensions` module is an add-on for your logback context.
This module does the following things:

* `DefaultHttpGetAppender`: the DefaultHttpGetAppender can send some GET requests to an configured requestUrl if something is logged.
* `HockeyAppCrashAppender`: the HockeyAppCrashAppender can send the stack trace of e.g. an exception of your program to hockeyapp, so hockey app can visualize your *exception flows* and create tickets in your bug tracker system automatically.

##Maven

Current version is available at central repository

    <dependencies>
        ...
        <dependency>
	        <groupId>com.mikewinkelmann</groupId>
		    <artifactId>logback-extensions</artifactId>
		    <version>0.0.2</version>
	    </dependency>
        ...
    </dependencies>

##Configuration
First you must configure your appender in your **logback.xml** - see below. As a last resort you must add your configured appender to your root appender:

    <root level="INFO">
		<appender-ref ref="HTTPGETAPPENDER" />
		<appender-ref ref="HOCKEYAPPCRASHAPPENDER" />
	</root>

###DefaultHttpGetAppender
This appender sends some log file with the specific message to the hockey app crash backend.
####Properties
Some properties are required some are optional

**required** properties are:

* `requestUrl` request url for the HTTP GET request

**optional** properties are:

* `loggingLevel` every log state you want to execute a GET call. Available states are the known: ERROR, WARN, INFO, DEBUG, TRACE you can also configure more than one log state with more loggingLevel tags - default: nothing
* `successStatusCodeMin` minimum successful http status code - default: 200
* `successStatusCodeMax` maximum successful http status code - default: 299
* `queueSize` size of the executing queue @TODO - default: 0

####Example  

    <appender name="HTTPGETAPPENDER" class="com.mikewinkelmann.logging.appender.http.DefaulthttpGetAppender">
		<requestUrl>http://localhost:8080/testservice</requestUrl>
		<loggingLevel>ERROR</loggingLevel>
		<loggingLevel>WARN</loggingLevel>
		<successStatusCodeMin>200</successStatusCodeMin>
		<successStatusCodeMax>299</successStatusCodeMax>
		<queueSize>0</queueSize>
	</appender>
	
###HockeyAppCrashAppender
This appender sends some log file with the specific message to the hockey app crash backend.
####Properties
Some properties are required some are optional

**required** properties are:

* `apiToken` your created apiToken on HockeyApp (Create: Profile/API Tokens)
* `appId` your appId from HockeyApp
* `packageName` the package name must match the bundle identifier set for the app on HockeyApp

**optional** properties are:

* `loggingLevel` every log state you want to execute a GET call. Available states are the known: ERROR, WARN, INFO, DEBUG, TRACE you can also configure more than one log state with more loggingLevel tags - default: nothing
* `successStatusCodeMin` minimum successful http status code - default: 200
* `successStatusCodeMax` maximum successful http status code - default: 299
* `queueSize` size of the executing reporting queue @TODO - default: 0
* `userId` a string with a user, deviceId ..., limited to 255 chars - default: empty
* `contact` a string with contact information ..., limited to 255 chars - default: empty
* `model` model of the device where this appender is running - default: empty
* `manufacturer` manufacturer of the device where this appender is running - default: empty
* `os` operating system of the device where this appender is running - default: empty
* `version` version of the application where this appender is running in - default: empty

####Example  

    <appender name="HOCKEYAPPCRASHAPPENDER" class="com.mikewinkelmann.logging.appender.http.DefaulthttpGetAppender">
		<!-- ##### general config ##### -->
		<loggingLevel>ERROR</loggingLevel>
		<successStatusCodeMin>200</successStatusCodeMin>
		<successStatusCodeMax>299</successStatusCodeMax>
		<queueSize>0</queueSize>
		
		<!-- ##### hockeyapp config ##### -->
		<apiToken>123Abc456deF789Ghi0</apiToken>
		<appId>1234567apId7654321</appId>
		<packageName>your.package.name</packageName>
		<userId>jeffrey</userId>
		<contact>test.test@test4711.com</contact>
		<model>m1.large</model>
		<manufacturer>amazon-aws</manufacturer>
		<os>cent os</os>
		<version>0.0.1</version>
	</appender>