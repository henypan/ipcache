# IP cache implementation

This project implements a cache structure for IP instances using **LRU** (least recently used) algrithm. 

#### 1. src/com.handy.ipcache.AddressCache.java
This is the interface of the address cache.

#### 2. src/com.handy.ipcache.AddressCacheImpl.java
This is the implmentation of the address cache.
a. Address cache can be instantiated with capacity and cachingTime(time-to-live, in miliseconds) set.  
b. By default, capacity is set as 10 and cachingTime is set as 0ms (meaning no auto-cleanup for the cache).  
c. **Time complexity** for each method implementation is in the comment on each method.

#### 3. test/com.handy.ipcache.AddressCacheImplTest.java
Unit test for AddressCache.


##### Note
This project is built using Java 1.7, no external libaries required.  
