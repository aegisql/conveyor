# conveyor
Asynchronous Builder Framework

[Tutorial](https://github.com/aegisql/conveyor/wiki)

Maven dependency
```xml
<dependency>
  <groupId>com.aegisql</groupId>
  <artifactId>conveyor</artifactId>
  <version>1.0.13</version>
</dependency>
```

## Release History

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
