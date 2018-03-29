# conveyor
Reactive Creational Framework

[Tutorial](https://github.com/aegisql/conveyor/wiki)

### Maven dependencies
conveyor-core
```xml
<dependency>
  <groupId>com.aegisql</groupId>
  <artifactId>conveyor-core</artifactId>
  <version>1.3.11</version>
</dependency>
```

conveyor-parallel
```xml
<dependency>
  <groupId>com.aegisql</groupId>
  <artifactId>conveyor-parallel</artifactId>
  <version>1.3.11</version>
</dependency>
```

conveyor-persistence
```xml
<groupId>com.aegisql.persistence</groupId>
  <artifactId>conveyor-persistence-core</artifactId>
  <version>1.3.11</version>
</dependency>

<dependency>
  <groupId>com.aegisql.persistence</groupId>
  <artifactId>conveyor-persistence-jdbc</artifactId>
  <version>1.3.11</version>
</dependency>
```

conveyor configurator (beta)
```xml
<dependency>
  <groupId>com.aegisql</groupId>
  <artifactId>conveyor-configurator</artifactId>
  <version>1.3.11</version>
</dependency>
```
## Release History

### 1.3.12-SNAPSHOT
* Added peek() and peek(Consumer) commands to AssemblingConveyor and CommandLoader
* Lazy Conveyor Supplier
* Lazy Persistence Supplier

### 1.3.11
* Derby persistence properties
* Extended support for JavaScript in Conveyor Persistence Configuration
* Define Conveyor and Persistence properties from ENV and System properties
* Added support for standard label readiness testers "readyWhenAccepted" properties
* unRegister conveyor name, to hide it from JMX and runtime lookup
* Forwarding results with foreach predicate option
* Bugfixes and improvements

### 1.3.10
* Started conveyor-configurator sub-project
* Create conveyors from .properties files
* Create conveyors from .YAML files
* Added constructor for L balanced conveyor that takes a list of enclosed conveyor names
* Created ForwardResult consumer factory
* Removed forwardTo methods from interface
* Resolving persistence by its JMX name: Persistence.byName(name)
* Bugfixes

### 1.3.9
* MBeans improvements
* MBean for the DerbyPersistence
* Conveyor.byName(name) method
* Persistence as an Archiver

### 1.3.7
* More converters for basic types
* File Archiver
* CartInputStream
* CartOurputStream
* ReflectingValueConsumer
* Simple Conveyor - uses reflection to match String labels to the Builder API
* @Label and @NoLabel annotations for builder setters
* Improvements and bugfixes

### 1.3.6
* Added converters for all basic Java types, collections, maps and arrays
* Improved restoring from persistence
* Added interrupt method to the Conveyor interface
* Added Interruptable interface.
* Bugfixes

### 1.3.4
* Persistence core module
* Persistence JDBC module
* Uses TimeHostIdGenerator by default
* Apache Derby implementation for the Persistence
* Serializable functional interfaces
* Changes in the Conveyor API to support persistence
* Acknowledge added to the ProductBin and ScrapBin
* General re-factoring
* Tests and bugfixes

### 1.3.3
* Extracted Parallel conveyors into a separate module
* Added the persistence project - testing deployment, not for use
* Added runtime properties

### 1.3.2
* No code changes. Changed project structure. All current code moved into the conveyor-core module 

### 1.3.1
* Added functional Interfaces ResultConsumer and ScrapConsumer with default methods extending standard Consumer interface.
* re-factored code to use new interfaces
* Cart interface now provides the getScrapConsumer method which can be overridden to customize the scrap consumption behavior.   
* Tests and bugfixes

### 1.3.0
* New unstable release
* Changes in the Result and Scrap consumer processing
* Removed multi-key part and command loader interfaces. Their functionality merged with part and command interfaces respectively.
* Added new and improved old result and scrap consumers
* More tests and bugfixes  

*** 

### 1.2.5
* Collection of result consumers
* Collection of scrap consumers

### 1.2.4
* CompletableFuture<Boolean> completeAndStop() method. When called, conveyor stops accepting new messages or commands, then waits for completion of existing tasks. When all builds complete or time out, exits the conveyor thread. This method can be useful for single time jobs, like massive batch load from parallel sources.
* Changes in the forwardResultsTo method. Now allows to forward results to conveyor with different data structure, inluding transformation of the key.
* Dropped ChainResults class as redundant. Functionality better covered by the forwardResultsTo methods

### 1.2.3
* static loader and values
* Improved setting names for smart lables
* bare smart labels with no actual function attached in order to pass messages that do not need to change state of the builder
* Improvements to Conveyor interface
* tests
* map-reduce demo

### 1.2.2
* added method getConsumerFor with explicit Bulder class parameter to avoid class casting in consumers.
* regex pattern match methods added to the LabeledValueConsumer interface.
* more tests
* minor bugfixes and refactoring

### 1.2.1
* Utility conveyors modified to use Loaders
* Extended use of Duration in conveyor settings
* Bugfixes
* More tests

### 1.2.0
* Changes in Conveyor interface
* Changes in Cart interface
* Loaders for parts, commands, futures and builds
* Bugfixes and improvements 

*** 

### 1.1.9
* More interface extensions
* Code re-factoring and cleaning

### 1.1.8
* Added default methods on basic functional interfaces: ProductSupplier, SmartLabel and LabeledValueConsumer. This allows to construct very flexible builders and data consumers

### 1.1.7
* Added explicit timeout postpone for TimeoutAction (if postpone enabled) to resolve possible conflicts with cache implementations  

### 1.1.6
* NOTE: This release contains a bug that affects caches with extendable TTL and builders that do not implement Expireable interface.
* Added timeout postpone from TimeoutAction (if postpone enabled)  
* minor bugfixes
* refactoring

### 1.1.5
* Added BuildTester - helping class to build readiness evaluator.  
* minor bugfix - double call of the ready() method


### 1.1.4
* New constructor for AssemblingConveyor allows to define Supplier for alternative implementation of internal Queues.  
* Helping classes for simple scalar caches

### 1.1.3
* ImmutableReference for simple scalar caches
* Bugfix in createBuild returned future 

### 1.1.2
* MBeans support 

### 1.1.1
* Bugfix in balancing functions. Also exists in 1.0.x 

### 1.1.0
* First and unstable release of the new version
* CompletableFuture on carts and commands
* CompletableFuture for the product
* Interface improvements
* ParallelConveyor becomes abstract and replaced by a pair of K and L balanced conveyors 
* Bugfixes for 1.0.x

*** 

### 1.0.16
* bugfix
* added posponing expiration time when data accessed via caching interface

### 1.0.15
* added posponing expiration time

### 1.0.14
* skip

### 1.0.13
* Changes in the Conveyor interface. Assembling and Parallel conveyors now share most of methods
* Scaling by labels
* setExpirationCollectionIdleInterval renamed to setIdleHeartBeat

### 1.0.12
* Bugfix
* Re-scheduling.  

### 1.0.11
* Chaining refactoring
* forEachKeyAndBuilder bugfix 

### 1.0.10
* createBuild interface
* Improvements for the CreateCart and CreateCommand
* forEachKeyAndBuilder method provides safe way to apply safe action to all current builds 

### 1.0.9
* Extended the Conveyor interface. added multiple add, offer and addCommand methods to avoid explicit creation of carts. Cart though is still a holder for all input information.
* Minor refactoring and tests

### 1.0.8
* Reschedule command
* DelayLine utility conveyor
* Improvement in expiration priority resolution

### 1.0.7
* Detailed failure type in the Scrap Bin

### 1.0.6
* add cart pre-placement user's validation algorithm to parallel conveyor
* add key pre-eviction user's algorithm to parallel conveyor

### 1.0.5
* add cart pre-placement user's validation algorithm
* add key pre-eviction user's algorithm
* minor timeout bugfix

### 1.0.4
* SmartLabel extends standard Supplier interface
* Added option to keep all accepted carts on site and make them available at testing and scrap collection time

### 1.0.3
* Added utility ResutQueue - Wrapper implementing Queue and ProductBin Consumer
* Changes scope of slf4j library to provided

### 1.0.2
* Moved excessive logging to TRACE level
* Delay queue now holds all Builders with the same expiration time in one Box. 

### 1.0.1
* Expireable is the only interface to control expiration time
* Updated libraries

### 1.0.0
* First release
