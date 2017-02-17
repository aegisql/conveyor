# conveyor
Asynchronous Builder Framework

[Tutorial](https://github.com/aegisql/conveyor/wiki)

Maven dependency
```xml
<dependency>
  <groupId>com.aegisql</groupId>
  <artifactId>conveyor</artifactId>
  <version>1.2.1</version>
</dependency>
```

## Release History

### 1.2.2-SNAPSHOT

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
