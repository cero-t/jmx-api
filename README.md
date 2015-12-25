# jmx-api
RESTful APIs for JMX. APIs are as follows.

## /
Get working java process ids like `jps` command.

### URL example
`http://localhost:8080/`

### Response example
```json
[
   {
      "pid":"13622",
      "name":"com.intellij.rt.execution.application.AppMain ninja.cero.jvm.jmxapi.MbeansApi",
      "simpleName":"AppMain"
   },
   {
      "pid":"11421",
      "name":"org.elasticsearch.bootstrap.Elasticsearch start",
      "simpleName":"Elasticsearch"
   },
   {
      "pid":"13661",
      "name":"org.jetbrains.idea.maven.server.RemoteMavenServer",
      "simpleName":"RemoteMavenServer"
   }
]
```

## /{pid}

Get mbeans of the target java process.

### URL example
`http://localhost:8080/12345`

### Response example
```json
[
   "JMImplementation:type=MBeanServerDelegate",
   "com.sun.management:type=DiagnosticCommand",
   "com.sun.management:type=HotSpotDiagnostic",
   "java.lang:name=CMS Old Gen,type=MemoryPool",
   "java.lang:name=Code Cache,type=MemoryPool",
   "java.lang:name=CodeCacheManager,type=MemoryManager",
   "java.lang:name=Compressed Class Space,type=MemoryPool",
   "java.lang:name=ConcurrentMarkSweep,type=GarbageCollector",
   "java.lang:name=Metaspace Manager,type=MemoryManager",
   "java.lang:name=Metaspace,type=MemoryPool",
   "java.lang:name=Par Eden Space,type=MemoryPool",
   "java.lang:name=Par Survivor Space,type=MemoryPool",
   "java.lang:name=ParNew,type=GarbageCollector",
   "java.lang:type=ClassLoading",
   "java.lang:type=Compilation",
   "java.lang:type=Memory",
   "java.lang:type=OperatingSystem",
   "java.lang:type=Runtime",
   "java.lang:type=Threading",
   "java.nio:name=direct,type=BufferPool",
   "java.nio:name=mapped,type=BufferPool",
   "java.util.logging:type=Logging"
]
```

## /{pid}/info

Get system properties and agent properties of the target java process.

### URL example
`http://localhost:8080/12345/info`

### Response example
```json
{
   "systemProperties":{
      "java.vendor":"JetBrains s.r.o",
      "com.apple.mrj.application.live-resize":"false",
      "idea.executable":"idea",
      "sun.management.compiler":"HotSpot 64-Bit Tiered Compilers",
      "sun.nio.ch.bugLevel":"",
      "idea.paths.selector":"IdeaIC15",
      "os.name":"Mac OS X",
      "java.specification.version":"1.8",
      "sun.java2d.d3d":"false"
   },
   "agentProperties":{
      "sun.jvm.args":"-Dfile.encoding=UTF-8 -XX:+UseConcMarkSweepGC -XX:SoftRefLRUPolicyMSPerMB=50 -ea -Dsun.io.useCanonCaches=false -Djava.net.preferIPv4Stack=true -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -Xverify:none -Xbootclasspath/a:../lib/boot.jar -Xms128m -Xmx750m -XX:MaxPermSize=350m -XX:ReservedCodeCacheSize=240m -XX:+UseCompressedOops -Djb.vmOptionsFile=/Applications/IntelliJ IDEA 15 CE.app/Contents/bin/idea.vmoptions -Didea.java.redist=custom-jdk-bundled -Didea.home.path=/Applications/IntelliJ IDEA 15 CE.app/Contents -Didea.executable=idea -Didea.paths.selector=IdeaIC15",
      "sun.jvm.flags":"",
      "sun.java.command":""
   }
}
```

## /{pid}/{mbean}

Get the list of mbean info and attributes, operations, constructors and notifications of the target java process.

### URL example
`http://localhost:8080/12345/java.lang:name=Par%20Survivor%20Space,type=MemoryPool`

### Response example
```json
{
   "description":"Information on the management interface of the MBean",
   "interfaceClassName":"java.lang.management.MemoryPoolMXBean",
   "attributes":[
      {
         "name":"Valid",
         "type":"boolean",
         "readable":true,
         "writable":false
      },
      {
         "name":"PeakUsage",
         "type":"javax.management.openmbean.CompositeData",
         "readable":true,
         "writable":false
      },
      {
         "name":"MemoryManagerNames",
         "type":"java.lang.String[]",
         "readable":true,
         "writable":false
      },
      {
         "name":"Usage",
         "type":"javax.management.openmbean.CompositeData",
         "readable":true,
         "writable":false
      },
      {
         "name":"UsageThresholdSupported",
         "type":"boolean",
         "readable":true,
         "writable":false
      },
      {
         "name":"ObjectName",
         "type":"javax.management.ObjectName",
         "readable":true,
         "writable":false
      }
   ],
   "operations":[
      {
         "name":"resetPeakUsage",
         "argTypes":[

         ],
         "returnType":"void"
      }
   ],
   "constructors":[

   ],
   "notifications":[

   ]
}
```

## /{pid}/{mbean}/{attributes}

Get one ore more mbean attributes of the target java process.

### URL example
`http://localhost:8080/12345/java.lang:name=Par%20Survivor%20Space,type=MemoryPool/attribute/Valid,PeakUsage,Usage`

### Response example
```json
{
   "Valid":true,
   "Usage":{
      "committed":4456448,
      "init":4456448,
      "max":26214400,
      "used":3481792
   },
   "PeakUsage":{
      "committed":4456448,
      "init":4456448,
      "max":26214400,
      "used":4456448
   }
}
```

## /{pid}/{mbean}/{attribute}?{argument}

Set the value to the mbean attribute of the target java process.

### URL example
`TBD`

### Response example
```
TBD
```

## /{pid}/{mbean}/{operation}?{arguments}

Execute the mbean operation of the target java process.

### URL example
`http://localhost:8080/12345/java.lang:type=Threading/operation/getThreadInfo?arg1=25,26&arg2=3`

### Response example
```json
[
   {
      "blockedCount":54731,
      "blockedTime":-1,
      "inNative":false,
      "lockInfo":{
         "className":"java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject",
         "identityHashCode":1357981065
      },
      "lockName":"java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject@50f12589",
      "lockOwnerId":-1,
      "lockOwnerName":null,
      "lockedMonitors":[

      ],
      "lockedSynchronizers":[

      ],
      "stackTrace":[
         {
            "compositeType":{
               "className":"javax.management.openmbean.CompositeData",
               "description":"java.lang.StackTraceElement",
               "typeName":"java.lang.StackTraceElement",
               "array":false
            }
         },
         {
            "compositeType":{
               "className":"javax.management.openmbean.CompositeData",
               "description":"java.lang.StackTraceElement",
               "typeName":"java.lang.StackTraceElement",
               "array":false
            }
         },
         {
            "compositeType":{
               "className":"javax.management.openmbean.CompositeData",
               "description":"java.lang.StackTraceElement",
               "typeName":"java.lang.StackTraceElement",
               "array":false
            }
         }
      ],
      "suspended":false,
      "threadId":25,
      "threadName":"AWT-EventQueue-0 15.0.2#IC-143.1184.17, eap:false",
      "threadState":"WAITING",
      "waitedCount":126465,
      "waitedTime":-1
   },
   {
      "blockedCount":46,
      "blockedTime":-1,
      "inNative":false,
      "lockInfo":{
         "className":"java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject",
         "identityHashCode":539408015
      },
      "lockName":"java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject@2026b68f",
      "lockOwnerId":-1,
      "lockOwnerName":null,
      "lockedMonitors":[

      ],
      "lockedSynchronizers":[

      ],
      "stackTrace":[
         {
            "compositeType":{
               "className":"javax.management.openmbean.CompositeData",
               "description":"java.lang.StackTraceElement",
               "typeName":"java.lang.StackTraceElement",
               "array":false
            }
         },
         {
            "compositeType":{
               "className":"javax.management.openmbean.CompositeData",
               "description":"java.lang.StackTraceElement",
               "typeName":"java.lang.StackTraceElement",
               "array":false
            }
         },
         {
            "compositeType":{
               "className":"javax.management.openmbean.CompositeData",
               "description":"java.lang.StackTraceElement",
               "typeName":"java.lang.StackTraceElement",
               "array":false
            }
         }
      ],
      "suspended":false,
      "threadId":26,
      "threadName":"Periodic tasks thread",
      "threadState":"TIMED_WAITING",
      "waitedCount":31690,
      "waitedTime":-1
   }
]
```
