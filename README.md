[![Project James](assets/james-logo.png "Project James")]()

[![License](http://img.shields.io/badge/license-APACHE2-blue.svg)]()

## Table of contents

* [Introduction](#intro)
* [How does it work?](#how)
    * [Information Points](#how-ips)
    * [Controllers](#how-controllers)
    * [Events](#how-events)
    * [Publishers](#how-publishers)
    * [Performance](#how-performance)
    * [Deployment](#how-deployment)
* [James setup and configuration](#conf)
    * [Agent configuration](#conf-agent)
    * [Controller plugins](#conf-controllers)
        * [Webservice Controller](#conf-controllers-ws)
        * [Consul Controller](#conf-controllers-consul)
    * [Publisher plugins](#conf-publishers)
        * [Console Publisher](#conf-publishers-console)
        * [File Publisher](#conf-publishers-file)
        * [Kinesis Publisher](#conf-publishers-kinesis)
    * [Toolkit Plugins](#conf-toolkits)
        * [Example toolkit](#conf-toolkits-example)
    * [Complete configuration example](#conf-example)
* [Information Point scripts](#scripts)
    * [Script example](#scripts-example)
* [Contributions](#contribs)


<a id='intro'></a>
## Introduction

*James* is a Java agent that may be attached to any JVM process and provide operational insight on what's going on 
under the hood of your service. 

It may extract the state of execution of any running method, including method arguments, object state, stack trace, 
call duration, transform that state into meaningful information using plugin toolkits or custom, hot-pluggable 
scripts, and finally load the transformation result into destination data warehouse or a file.

<a id='how'></a>
## How does it work?

*James* uses [ByteBuddy](http://bytebuddy.net/) code generation and manipulation library for injecting advices into
underlying application bytecode.

You can wire up *James* to your service by providing JARs with the agent and its plugins and adding two parameters 
to the virtual machine. Once the service is running *James* provides a way to manage a set of *Information Points* 
through a *Controller*.

<a id='how-ips'></a>
### Information Points
*Information Point* defines a point in your code where *James* hooks up to a running service. Think of it as an 
advice or an aspect wrapped around a method. When the method with an *Information Point* is called, *James* 
measures method execution time and invokes a Groovy script provided as a part of an *Information Point* definition. 
In that script you can transform the state of method parameters or method's object to anything you like and publish 
the result of mapping as an *Event*. 

For example, if a method contains transaction ID in its arguments, you could call some kind of metadata service and 
retrieve metadata for that transaction ID, and publish it in your Event.

Beside a reference to method, at which *Information Point* has to be created and a script, that has to be executed 
when the method is executed, a *Information Point* may have a *sampleRate* parameter, which translates to 
a probability at which the advise will invoke the script. For example when *sampleRate* is set to 50, only half 
of method invocations will cause the script to execute. This is designed to enable throttling of outbound message 
throughput for methods that are executed too frequently.

<a id='how-controllers'></a>
### Controllers
*Controllers* enabled you to interact with *James* by listing, adding and removing *Information Points* and by 
querying diagnostic information.

There are two kinds of *Controllers* at the moment – *HTTP Web Service Controller* and *Consul Controller*. 
The former enables managing *Information Points* on single agent-enabled service via REST interface, the latter 
uses Consul node as a source of *Information Point* configuration for a cluster of agent-enabled services. 
*Controllers* enable rapid deployment of a set of *Information Points*.

<a id='how-events'></a>
### Events
*Event* is a structured piece of information published by a Groovy script defined for *Information Point* that 
can be loaded into either text file in any format (JSON formatter is provided) or through Kinesis to ElasticSearch. 
That way all information gathered from *Information Points* is easily available in Kibana for analysis.

<a id='how-publishers'></a>
### Publishers
A *Publisher* is a component responsible for sending your *Events* to the outside world. It may be 
*Console Publisher* that will simply write JSON-formated *Events* to `stdout`, it may be *File Publisher* that will 
put *Events* into a file, or it may be *Kinesis Publisher* that will send your *Events* to 
[AWS Kinesis](https://aws.amazon.com/kinesis/) for buffering and further processing.

<a id='how-performance'></a>
### Performance
*James* is designed with performance in mind, the execution time overhead it introduces to the method invocations 
is minimal and all I/O and data processing by Groovy script engine is done in the background. Our microbenchmarks 
let us believe that it can handle about 150k *Information Point* hits per second on a laptop, thus effectively 
for most use cases its performance is bound by a *Publisher* message throughput.

<a id='how-deployment'></a>
### Deployment
At TomTom Content Production Platform we use *James* with our services to gather runtime information for analysis 
and troubleshooting purposes. The configuration of the cluster of *James*-enabled services is managed using 
*Consul Controller*. *Kinesis Publisher* is used to send *Events* to [AWS Kinesis](https://aws.amazon.com/kinesis/) 
stream from which they are picked by [AWS Lambda](https://aws.amazon.com/lambda/), put into 
[Elasticsearch](https://www.elastic.co/products/elasticsearch) cluster and visualized using 
[Kibana](https://www.elastic.co/products/kibana). With this setup we gather more than 30 GB of valuable runtime 
information from our system each day and analyze it in near-realtime with Kibana.


<a id='conf'></a>
## James setup and configuration
To start working with *James* you need to add two command line arguments to Java Virtual Machine running your 
application:

```
-javaagent:/path/to/james-agent.jar -Djames.configurationPath=/path/to/james-configuration.yaml
```

<a id='conf-agent'></a>
### Agent configuration
The configuration of *James* is an YAML-formatted document made of several sections that contain settings of 
the agent's subsystems. Most of settings have default values, thus minimal configuration may look like this:

```yaml
plugins:
  includeDirectories:
    - /opt/james/plugins/common             # Look for plugins in this directory at start
    - /opt/james/plugins/service-specific   # ... and in this directory

controllers:
  - id: james.controller.webservice         # Use Webservice Controller for management with default settings

publishers:
  - id: james.publisher.console             # Use Console Publisher to write JSON-formatted events to stdout with
                                            # default settings
```

Sections of `controllers`, `publishers` and `toolkits` are described below, let's take a look at more complete 
example of core settings configuration:

```yaml
quiet: true                                     # Don't print out ASCII banner at the start (default: false)
logLevel: trace                                 # Level of James logs - trace|debug|info|warn|error|fatal
                                                # (default: warn)

plugins:
  includeDirectories:
    - /opt/james/plugins                        # Look for plugins in this directory at start
  includeFiles:
    - /opt/james-extra/additional-plugin1.jar   # ... and in this JAR file
    - /opt/james-extra/additional-plugin2.jar   # ... and in this JAR file

controllers:
  - id: james.controller.webservice
    properties:
      # See below for james.controller.webservice configuration properties.
  - id: james.controller.consul
    properties:
      # See below for james.controller.consul configuration properties.

publishers:
  - id: james.publisher.console
    properties:
      # See below for james.publisher.console configuration properties.
  - id: james.publisher.file
    properties:
      # See below for james.publisher.file configuration properties.
  - id: james.publisher.kinesis
    asyncWorkers: 4                     # Use 4 worker threads for consuming Events by this Publisher 
                                        # (default: 1).
    maxAsyncJobQueueCapacity: 20000     # Maximum size of job queue for this Publisher is 20000 Events
                                        # (default: 10000). If the queue is full when a script tries 
                                        # to publish an Event, the Event will be dropped.
    properties:
      # See below for james.publisher.kinesis configuration properties

toolkits:
  yourOrganization.toolkit.yourToolkit:
      # See below for toolkit configuration properties.

scriptEngine:
  asyncWorkers: 4                       # Run 4 instances of the Script Engine concurrently (default: # of cpus).
  maxAsyncJobQueueCapacity: 10000       # Maximum size of job queue for the Script Engine (default: 10000).

informationPointStore:
  persistenceEnabled: true                          # Should Information Points be persisted between restarts
                                                    # (default: false).
  storeFilePath: /var/james/informationpoints.json  # If Information Point persistence is enabled, use this file 
                                                    # for storage.
```

Two plugins are built-in into *James* and do not require provisioning as separate plugin JARs: 
`james.publisher.console` and `james.publisher.file`.


<a id='conf-controllers'></a>
### Controller plugins
The role of *Controller plugins* is to provide an interface for interactions with *James*. 

There may be any number of *Controller plugins* running at the same time in a single *James* instance.


<a id='conf-controllers-ws'></a>
#### Webservice Controller
*Webservice Controller* enables you to manage a single instance of *James* using HTTP REST interface. You can list, 
add, remove an *Information Point* or check the state of internal queues used for asynchronous communication 
between James' subsystems.

By default the controller listens for HTTP requests on TCP port 7007. You can configure the controller using the
following configuration properties:

```yaml
plugins:
  includeFiles:
    - /path/to/plugins/james-controller-webservice.jar

controllers:
  - id: james.controller.webservice
    properties:
      port: 7007        # TCP port on which the controller is listening for connections (default: 7007)
      minThreads: 1     # Minimal number of HTTP server worker threads (default: 1) 
      maxThreads: 8     # Maximal number of HTTP server worker threads (default: 8) 
```

Following HTTP commands are supported:

Request Path                                   | HTTP Verb | Request Media Type | Response Media Type | Description
---------------------------------------------- | ----------| ------------------ | ------------------- | -----------
/v1/information-point                          | GET       | N/A                | application/json    | Get definitions of all *Information Points*.
/v1/information-point/{className}/{methodName} | GET       | N/A                | application/json    | Get a definition of specific *Information Point*.
/v1/information-point                          | POST      | application/json   | N/A                 | Create an *Information Point*. See below for an example of payload.
/v1/information-point/{className}/{methodName} | DELETE    | N/A                | N/A                 | Delete an *Information Point*.
/v1/queue                                      | GET       | N/A                | application/json    | Get an information about all async queues.
/v1/queue/script-engine                        | GET       | N/A                | application/json    | Get an information about *Script Engine* async queue.
/v1/queue/event-publisher                      | GET       | N/A                | application/json    | Get an information about *Event Publisher* async queue.

The body of "POST /v1/information-point" command that adds and *Information Point* at 
`com.github.me.myapp.MyClass#myMethodInMyClass` with 30% sampling rate and provided script may look like this:

```json
{
    "className": "com.github.me.myapp.MyClass",
    "methodName": "myMethodInMyClass",
    "script": [
        "// First line of Information Point script",
        "// Second line of Information Point script"
    ],
    "sampleRate": 30
}
```

If you need a simple CLI manager for *James* that uses *Webservice Controller* take a look at 
[james-m](https://github.com/pdebicki/james-m) project. The tool is self-describing with its built-in help system. 
Some usage examples:

* Add a new *Information Point* at *James* running with *Webservice Controller* on `100.101.102.103:17007` with 
*sample rate* 20% and `script1.groovy` script:
```
james-m --host 100.101.102.103 --port 17007 ip add \
        com.github.me.myapp.MyClass#myMethodInMyClass \
        scripts/script1.groovy \
        --sample-rate 20
```

* Remove an *Information Point* at *James* running with *Webservice Controller* on `localhost:7007` (default):
```
james-m ip remove com.github.me.myapp.MyClass#myMethodInMyClass
```

* Show all *Information Points* at *James* running with *Webservice Controller* on `localhost:7007` (default):
```
james-m ip show
```

*James-m* is [available](https://github.com/pdebicki/james-m/releases) as Windows executable. Alternatively you can 
build it yourself using `go get github.com/pdebicki/james-m` if you have [Go](https://golang.org/) build 
environment installed.


<a id='conf-controllers-consul'></a>
#### Consul Controller
With *Consul Controller* you can add, remove or modify *Information Points* using [Consul](https://www.consul.io/)
key-value store. Each *Information Point* is stored as a key-value pair, where the key is a method reference
of *Information Point* (for example `com.github.me.myapp.MyClass!myMethodInMyClass`) and the value is 
a JSON-formatted definition of the *Information Point*. 

When the KV store entry is added in Consul, all *James* instances with *Consul Controller* automatically add
an *Information Point* defined in that KV entry. When the entry is modified, all instances replace the 
*Information Point* and when the entry is removed, all instances remove the *Information Point*.

Note that unlike other use cases, the type name and the method name in method reference string are separated 
with `!` character instead of common `#` character. That's because Consul does not allow `#` character in its keys.

You can configure *Consul Controller* using the following configuration properties:

```yaml
plugins:
  includeFiles:
    - /path/to/plugins/james-controller-consul.jar

controllers:
  - id: james.controller.consul
    properties:
      host: localhost                               # Host of the Consul node (default: localhost)
      port: 8500                                    # Port of the Consul node (default: 8500)
      folderPath: james/test/information-points     # A path to the Consul KV folder containing Information Points
```

An example of Consul KV store value containing *Information Point* definition could be:

```json
{
    "version": 1,
    "script": [
        "// First line of Information Point script",
        "// Second line of Information Point script"
    ],
    "sampleRate": 30
}
```

For the above example, the key value could be `com.github.me.myapp.MyClass!myMethodInMyClass`. The `version` 
property is the version of the JSON schema, intended to maintain backward compatibility of existing
*Information Point* definitions.


<a id='conf-publishers'></a>
### Publisher plugins
The role of a *Publisher* is to marshall and send out an *Event* generated by an *Information Point script*.
All *Publishers*, same way as the *Script Engine* are asynchronous; the advise on *Information Point*'s method only 
captures the context of method execution and schedules execution of the script in the *Script Engine* thread pool.
Same way *Events* created by *Information Point scripts* are scheduled for future processing by one or more 
*Publishers* in the background. That way the impact of *James* on the running application is minimal.

A single instance of *James* may have any number of *Publishers*.

<a id='conf-publishers-console'></a>
#### Console Publisher
*Console Publisher* enables writing of *Events* generated by *James* to `stdout`. It is built-in into *James* agent
JAR, thus plugin provisioning is not required to use it.

The configuration is very simple:

```yaml
publishers:
  - id: james.publisher.console
    properties:
      prettifyJSON: true        # Whether the output JSON should be pretty-printed (default: false).
``` 

<a id='conf-publishers-file'></a>
#### File Publisher
*File Publisher* enables writing of *Events* generated by *James* to a file. It is built-in into *James* agent
JAR, thus plugin provisioning is not required to use it.

The configuration is also very simple:

```yaml
publishers:
  - id: james.publisher.file
    properties:
      prettifyJSON: true                    # Whether the output JSON should be pretty-printed (default: false).
      path: /var/james/file-publisher.out   # Output file path (default: $HOME/james-publisher-file-output.json).
``` 

<a id='conf-publishers-kinesis'></a>
#### Kinesis Publisher
*Kinesis Publisher* enables sending *Events* generated by *James* to [AWS Kinesis](https://aws.amazon.com/kinesis/)
stream. The stream may serve as a buffer, for example between *James* instances and 
[Elasticsearch](https://www.elastic.co/products/elasticsearch) cluster.

Published *Events* are marshalled as JSON documents, with additional fields like `@timestamp`, `@version`, 
`host`, `jvmName`, `type`, and `environment` that enable identification of *Event* source and sending *Events* to 
Elasticsearch cluster without additinal data structure transformations. Fields `type` and `environment` are 
configurable and `@timestamp`, `@version`, `host`, `jvmName` are auto-generated.

The structure of the configuration is as in the following example:

```yaml
plugins:
  includeFiles:
    - /path/to/plugins/james-controller-kinesis.jar

publishers:
  - id: james.publisher.kinesis
    asyncWorkers: 4                     # Use 4 worker threads for consuming Events by this Publisher 
                                        # (default: 1).
    maxAsyncJobQueueCapacity: 20000     # Maximum size of job queue for this Publisher is 20000 Events
                                        # (default: 10000). If the queue is full when a script tries
                                        # to publish an Event, the Event will be dropped.
    properties:
      stream: stream                    # Name of the target Kinesis stream (default: james-kinesis).
      partitionKey: partitionkey        # Stream partition key (default: random UUID).
      elasticSearch:
        eventType: james                # Type of the Event for Elasticsearch (default: james).
        environment: dev-env            # Name of the environment for Elasticsearch (only sent if present).
      producer:                         # Optional. See below.
        aggregationEnabled: true
        aggregationMaxCount: 4294967295
        aggregationMaxSize: 51200
        cloudwatchEndpoint: cloudwatch-endpoint
        cloudwatchPort: 443
        collectionMaxCount: 500
        collectionMaxSize: 5242880
        connectTimeout: 6000
        enableCoreDumps: false
        failIfThrottled: false
        kinesisEndpoint: kinesis-endpoint
        kinesisPort: 443
        logLevel: info
        maxConnections: 10
        minConnections: 1
        nativeExecutable: native-exec
        rateLimit: 150
        recordMaxBufferedTime: 100
        recordTtl: 30000
        region: region
        requestTimeout: 6000
        tempDirectory: /tmp
        verifyCertificate: true
```

All the properties in `producer:` section of the configuration enable overriding of default settings in 
[amazon-kinesis-producer](https://github.com/awslabs/amazon-kinesis-producer/) library. Check [default_config.properties](https://github.com/awslabs/amazon-kinesis-producer/blob/master/java/amazon-kinesis-producer-sample/default_config.properties)
for description of each property and default value.


<a id='conf-toolkits'></a>
### Toolkit Plugins
*Toolkits* enable provisioning of external libraries to *James* for use in *Information Point scripts*. 
*Toolkit* may come with it's own set of third party dependencies, if bundled together into a single JAR.

<a id='conf-toolkits-example'></a>
#### Example toolkit
Let's consider an example. You might want to extract a transactionID from method arguments of your application, 
call a metadata service to fetch some kind of metadata for that transaction, perform some additional processing 
of the data and publish an *Event* containing the results. To do that you will either need to implement metadata 
service client in Groovy or use existing client. Using *Toolkit* you can wrap an existing client in *James* plugin
and call it from *Information Point script* code. 

To create a *Toolkit* you need to implement `com.tomtom.james.common.api.toolkit.Toolkit` from `james-agent-common`
module.

```java
public class MetadataServiceToolkit implements Toolkit {

    private RealMetadataClient realMetadataClient;

    @Override
    public String getId() {
        return "yourOrganization.toolkit.metadataService";
    }

    @Override
    public void initialize(ToolkitConfiguration toolkitConfiguration) {
        StructuredConfiguration configuration = toolkitConfiguration.getProperties()
                .orElseGet(StructuredConfiguration.Empty::new);

        String serviceURL = configuration.get("serviceURL")
                .map(StructuredConfiguration::asString)
                .orElseThrow(() -> new ConfigurationStructureException("Missing serviceURL property"));

        realMetadataClient = new RealMetadataClient(serviceURL);
    }

    public Map<String, String> getMetadataForTransaction(UUID transactionID) {
        return realMetadataClient.getMetadataForTransaction(transactionID);
    }
}
```

In the example above the `initialize()` method expects the `serviceURL` property to be present in property 
set of your tookit. In that case *James* configuration might look like this:

```yaml
plugins:
  includeFiles:
    - /path/to/metadata-toolkit.jar

toolkits:
  yourOrganization.toolkit.metadataService:
    serviceURL: http://metadata-service.somewhere.org/
```

To call a method of a *Toolkit* you have to retrieve the *Toolkit* object from the *Script Engine* framework
using *Toolkit*'s ID, and then simply invoke the method using Groovy's dynamic nature.

```groovy
import com.tomtom.james.script.SuccessHandlerContext

def onSuccess(SuccessHandlerContext context) {
    def txnID = context.runtimeParameters[0].value

    def toolkit = getToolkitById("yourOrganization.toolkit.metadataService")
    def txnMetadata = toolkit.getMetadataForTransaction(txnID)
    def txnCreator = txnMetadata["creator"]

    def eventMap = [
        transaction_id:      txnID,
        transaction_creator: txnCreator
    ]
    publishEvent(new Event(eventMap))
}
```

<a id='scripts'></a>
## Information Point scripts
*Information Point script* is a piece of Groovy code that is invoked by the *Script Engine* after a method with 
defined  *Information Point* is executed. It has to contain at least two functions:

```groovy
import com.tomtom.james.script.SuccessHandlerContext
import com.tomtom.james.script.ErrorHandlerContext

def onSuccess(SuccessHandlerContext context) {
}

def onError(ErrorHandlerContext context) {
}
```

Function `onSuccess` is an event handler that is called by the *Script Engine* when the method returns a result.
Function `onError` is called when the method throws an exception.

Both `SuccessHandlerContext` and `ErrorHandlerContext` are derived from the common type - 
`InformationPointHandlerContext` and share a set of runtime information captured by the *Information Point* during
execution of the method.

Context property             | Type                                     |  Available in                  
-----------------------------|------------------------------------------|--------------
`informationPointClassName`  | `java.lang.String`                       | InformationPointHandlerContext
`informationPointMethodName` | `java.lang.String`                       | InformationPointHandlerContext
`origin`                     | `java.lang.reflect.Method`               | InformationPointHandlerContext
`runtimeInstance`            | `java.lang.Object`                       | InformationPointHandlerContext
`runtimeParameters`          | `List<RuntimeInformationPointParameter>` | InformationPointHandlerContext
`currentThread`              | `java.lang.Thread`                       | InformationPointHandlerContext
`executionTime`              | `java.time.Duration`                     | InformationPointHandlerContext
`callStack`                  | `java.lang.String[]`                     | InformationPointHandlerContext
`returnValue`                | `java.lang.Object`                       | SuccessHandlerContext
`errorCause`                 | `java.lang.Throwable`                    | ErrorHandlerContext

Properties `informationPointClassName` and `informationPointMethodName` contain class and method name of the 
*Information Point* that triggered the script. 

Property `origin` contains a reference to the executed method itself. Note the difference - if an 
*Information Point* was set on an abstract method or an overriden method of a base class then 
`informationPointClassName` will contain the name of superclass, and `origin.declaringClass.name` will contain 
the name of actual subclass of which the method was executed.

Property `runtimeInstance` contains a reference to the object of which the method was invoked. You can use it
to access fields or call methods on the object.

Property `runtimeParameters` has a list of all parameters of the method, elements of which contain parameter value, 
type and name.

Property `currentThread` contains a reference to a thead that executed the method. 

Property `executionTime` contains a time of method's execution, with precision of nanoseconds. 

Property `callStack` is an array of all method references in call stack of the thread that executed the method.

Property `returnValue` contains the return value of the method after successful execution.

Property `errorCause` contains the reference to Throwable that was thrown by the method.

Beside runtime properties passed to event handlers via a context there are two global functions defined for
*Information Point scripts*.

Function `void publishEvent(Event event)` sends an *Event* to *Publisher*'s queue for processing in the future.

Function `Toolkit getToolkitById(String toolkitId)` returns a reference to a *Toolkit* instance by given ID.

<a id='scripts-example'></a>
### Script example

```groovy
import com.tomtom.james.common.api.publisher.Event
import com.tomtom.james.script.ErrorHandlerContext
import com.tomtom.james.script.SuccessHandlerContext

def onSuccess(SuccessHandlerContext context) {
    def eventMap = [
            informationPointClassName : context.informationPointClassName,
            informationPointMethodName: context.informationPointMethodName,
            originDeclaringClassName  : context.origin.declaringClass.name,
            originName                : context.origin.name,
            instanceFieldValue        : context.instance.field,
            executionTimeNanos        : context.executionTime.toNanos(),
            callStack                 : context.callStack,
            currentThreadName         : context.currentThread.name,
            returnValue               : context.returnValue,
    ]
    context.parameters.each {
        eventMap["arg(${it.name})"] = it.value
    }
    publishEvent(new Event(eventMap))
}

def onError(ErrorHandlerContext context) {
    def eventMap = [
            informationPointClassName : context.informationPointClassName,
            informationPointMethodName: context.informationPointMethodName,
            originDeclaringClassName  : context.origin.declaringClass.name,
            originName                : context.origin.name,
            instanceFieldValue        : context.instance.field,
            executionTimeNanos        : context.executionTime.toNanos(),
            callStack                 : context.callStack,
            currentThreadName         : context.currentThread.name,
            errorCauseMessage         : context.errorCause.message,
    ]
    context.parameters.each {
        eventMap["arg(${it.name})"] = it.value
    }
    publishEvent(new Event(eventMap))
}
```


<a id='contribs'></a>
## Contributions

*James* is a young and small project created inside one of TomTom's development teams to give us more insight
on what's going on inside our services. It was meant to deliver information where big guys like AppDynamics failed 
to do so and give us lots of flexibility when we need to analyze and troubleshoot our running code.

Although the codebase is very young, *James* is running successfully on hundreds of EC2 machines and delivers 
value to us for several months.

We hope you will find *James* useful and welcome all contributions from the Open Source community.