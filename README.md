# dbstress
[![Build Status](https://travis-ci.org/semberal/dbstress.svg?branch=master)](https://travis-ci.org/semberal/dbstress)

## Introduction

_dbstress_ is an open-source database performance and stress testing tool written in [Scala](http://www.scala-lang.org/) and [Akka](http://akka.io). To put it short and simple, _dbstress_ runs a database query (using a database-specific JDBC driver) certain number of times in parallel (possibly against multiple database hosts) and reports (both exhaustive and summarized) results for further processing.


## Obtaining and running

Download the [latest release]() from the _dbstress_ [releases page]() and extract the tarball:

```bash
tar -zxvf dbstress-${version}.tgz
cd dbstress-${version}
```

_dbstress_ doesn't bundle any JDBC drivers in the standard distribution, so download the driver for the database system you would like to test and copy it into the `lib/` subdirectory. 

<!-- Consult the relevant _dbstress_ [wiki page]() for links to various common drivers. -->

In order to launch _dbstress_, you must provide two command line arguments: _scenario configuration_ and _output directory_:

```bash
bin/dbstress.sh -c /path/to/scenario_config.yaml -o /output/directory
```
(Windows users should use the executable `dbstress.bat` script, instead).

Scenario configuration is a [YAML](http://www.yaml.org/start.html) file ([example](https://github.com/semberal/dbstress/blob/master/src/test/resources/config1.yaml)) describing various aspects of the performance/stress test itself, such as database connection information, number of parallel connections, number of repeats, timeouts, etc.

Output directory specifies the directory the results should be exported to.

You can also run the application from the cloned Github repository using [sbt](http://scala-sbt.org). Just be sure to checkout the latest tag, because the master branch might not always be stable enough.

```bash
git clone git@github.com:semberal/dbstress.git
git checkout -b 1.0.0 1.0.0 # todo clone the tag directly
sbt run -c /path/to/scenario_config.yaml -o /output/directory
```

## Terminology and the test scenario description

Top level configuration element is a _scenario_, which consists of at least one _unit_. 
A _unit_ represents a particular database operation to be performed, along with its configuration.
All configured units within a scenario run in parallel, independently of each other and their results are also reported separately. Unless you need to do some more advanced testing, such as connecting to the database with different users or to different schemas, it is perfectly fine to have a scenario consisting just of a single unit.

The following unit parameters have to be specified in the YAML document:

* Unit name
* Description (Optional)
* Database query to be performed
* JDBC connection string
* Database username/password (Must be present even when empty)
* JDBC driver class name (Optional, it most cases it shouldn't be necessary)
* Number of parallel database connections (i.e. simultaneous database sessions) - further referred as `PAR`
* How many times should the query be repeated (various statistics are calculated from individual samples) - further referred as `REP`
* Query timeout (Optional, use with caution - see the [Threading](#threading) section for details
* Connection initiation timeout (Optional)

Most of the configuration options above should be clear, two most important ones which need further clarification are parallel connections and repeats.

The test run consists of two base phases: _connection initialization phase_ and _query execution phase_. In the _connection initialization phase_, each unit spawns `PAR` so called _unit runs_. Every _unit run_ opens a separate database connection (represented by the `java.sql.Connection` object). In the _query execution phase_, every unit run sends the configured query `REP` times to the database in a sequential way (one after another, not in parallel). Therefore, if nothing goes wrong (i.e. all connections are successfully initialized - see [below](#error-handling) for discussion what happens when some connections fail), there are in total `PAR*REP` queries sent to the database within a unit. Durations of these queries form a dataset, which (along with some aggregated statistical values) represents the result of a unit.

You can see the meaning of `PAR` and `REP` variables, as well as the overall description of individual components and how they parallelize in the following diagram:

![Components overview](images/Terminology.png?raw=true)


### Configuration

Scenario configuration contains one YAML document per unit (multiple documents in a single YAML file are separated using `---`). Here is an example of a scenario configuration with two units:
	
```yaml
---
unit_name: unit1
description: Example of a successful unit
query: "select 1"
uri: "jdbc:h2:mem://localhost"
driver_class: org.h2.Driver
username: sa
password: ""
parallel_connections: 2
repeats: 5
connection_timeout: 500
query_timeout: 500

---
unit_name: unit2
description: Example of a unit which cannot even establish a connection (wrong uri string)
query: select 2
uri: "foobar"
driver_class: org.h2.Driver
username: sa
password: ""
parallel_connections: 10
repeats: 5
connection_timeout: 50
query_timeout: 50
```

### Threading
In order to fine tune the performance tests, it is useful to know how the mapping of database worker jobs to threads works. In _dbstress_, there are two thread pools. The first one is used mostly for scenario orchestration and results aggregation. It is the [default Akka dispatcher](http://doc.akka.io/docs/akka/current/scala/dispatchers.html#Default_dispatcher), which can spawn up to 64 threads (it will usually be much less). The other thread pool is used for execution of the blocking database calls themselves. By default, the number of threads in this thread pool ranges (i.e. new threads are spawned when necessary) from the total parallel connections count configured for all units to the total number of parallel connections multiplied by 1.5. The total number of thread in this thread pool can be configured by the `--max-db-threads` command line parameter.

Why are the minimum and maximum values chosen this way? The minimum value is obvious, there has to be at least as many threads as parallel database calls. If there were no query timeouts, the maximum thread pool size could be equal to the minimum size. However, if you define the `query_timeout` unit parameter, even though queries running longer than the configured limit will be reported as timeout in the scenario results, their execution will not be terminated. Sometimes it is not possible (`pg_sleep()` or when the database has not yet returned a cursor) to do so, but it is not yet implemented even in cases when it is possible (already iterating over the database cursor).

This is the reason why it is recommended to use query timeouts with great caution, timeouted queries will be correctly reported in the scenario results, the database operations, however, will not be interrupted and thus create additional database load which, consequently, can lead to biased results.

### Error handling

Various kinds of errors can occur during a scenario run, two most important categories of errors are _connection initialization errors_ and _query errors_.

When a connection initialization fails (either due to an exception or a timeout), the unit run cannot proceed to the _query execution phase_ and send its portion of `REP` database queries. Therefore, the the total number of queries sent to the database in a unit will be `(PAR-N)*REP`, where `N` represents the number of failed _unit runs_. This is the reason why the unit summary might report the number of expected db calls to be larger than the number of db calls actually sent.

Query errors, on the other hand, are nothing special, failed queries are simply reported as failures and unit run proceeds to the next iteration (obviously unless the query has already been executed `REP` times already).

Information about both _connection initialization errors_ and _query errors_, along with the reasons (i.e. exception messages) are contained in the complete scenario results.

When the application completes successfully, it exits with status `0` (successful completion means the scenario has completed as expected, regardless of all database errors). Exit status `1` means incorrect command line arguments have been passed and exit status `2` represents invalid scenario configuration (YAML file). 

## Results

When a scenario run finishes, it creates two files in the output directory: `summary.${timestamp}.csv` and `complete.${timestamp}.json`.

The CSV summary file doesn't contain details about individual database calls, but rather statistical values (min, max, mean, median and standard deviation) calculated from all/successful/failed samples. The JSON report contains summarized statistical values, as well, but unlike the CSV report it also contains exhaustive information about individual database calls (start/end time, number of rows fetched, updated rows count, etc).

After some consideration I decided to choose CSV and JSON formats to represent the _dbstress_ results, because they cover two slightly different uses cases. CSV summarized reports are very convenient when you would like to just copy&paste them into an Excel spreadsheet and create some quick performance graphs. JSON report, on the other hand, is less readable, but can be easily imported into _MongoDB_ or similar database and some more advanced reporting can be built on top of it.

## Issues
If you have any problem with the application, find a bug or encounter an unexpected behaviour, please [create an issue](https://github.com/semberal/dbstress/issues/new) in the _dbstress_ [issue tracker](https://github.com/semberal/dbstress/issues) on GitHub.

<!--
## Roadmap

### 2.0
Main theme of the 2.0 release will be distributed testing. It is often the case you have several workers and you would
like to point them all 
-->

<!--
## Technical description

_dbstress_ is written in the Scala programming language and is implemented using Akka actors. The following diagram describes the actor hierarchy:

![dbstress actors](./images/Actors.png)
-->

<!--
## F.A.Q.

### Does dbstress support non-relational databases, such as MongoDB, as well?
 
Unfortunately, it doesn't. Currently _dbstress_ only supports JDBC access and there is no plan implement support for non-JDBC databases. Community contributions are always welcome, though.
-->
