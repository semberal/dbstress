# dbstress
[![Build Status](https://travis-ci.org/semberal/dbstress.svg?branch=master)](https://travis-ci.org/semberal/dbstress)

## Introduction

_dbstress_ is an open-source database performance and stress testing tool written in [Scala](http://www.scala-lang.org/) and [Akka](http://akka.io). To put it short and simple, _dbstress_ runs a database query (using a database-specific JDBC driver) certain number of times in parallel (possibly against multiple database hosts) and reports (both exhaustive and summarized) results for further processing.


## Obtaining and running

Download the latest release from the _dbstress_ [releases page](https://github.com/semberal/dbstress/releases) and extract the tarball:

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

Scenario configuration is a [YAML](http://www.yaml.org/start.html) file ([example](https://github.com/semberal/dbstress/blob/master/src/it/resources/config1.yaml)) describing various aspects of the performance/stress test itself, such as database connection information, number of parallel connections, number of repeats, timeouts, etc.

Output directory specifies the directory the results should be exported to.

You can also run the application from the cloned Github repository using [sbt](http://scala-sbt.org). Just be sure to checkout the latest tag, because the master branch might not always be stable enough.

```bash
git clone git@github.com:semberal/dbstress.git
sbt "run -c /path/to/scenario_config.yaml -o /path/to/output_directory"
```

_dbstress_ requires Java 8.

## Terminology and the test scenario description

Top level configuration element is a _scenario_, which consists of at least one _unit_. 
A _unit_ represents a particular database operation to be performed, along with its configuration.
All configured units within a scenario run in parallel, independently of each other and their results are also reported separately. Unless you need to do some more advanced testing, such as connecting to the database with different users or to different schemas, it is perfectly fine to have a scenario consisting just of a single unit.

The following unit parameters have to be specified in the YAML document:

* Unit name (must be an alphanumeric string)
* Description (Optional)
* Database query to be performed
* JDBC connection string
* Database username/password (Must be present even when empty unless a default password is passed on command line)
* JDBC driver class name (Optional, in most cases it shouldn't be necessary)
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
```

### Threading
In _dbstress_, there are two important thread pools. The first one is used by the Akka actor system infrastructure
(for scenario orchestration, results aggregation, etc) and can spawn up to 64 threads (it will be usually much less).

The other thread pool is used for execution of the blocking database calls.
The number of threads in this thread pool is equal to the sum of database connections across all units.
In other words, there is a dedicated thread for every database connection.

### Database calls labelling

In order to identify individual database calls in the server logs, you might want to place a random identifier into each query. _dbstress_ supports query labeling with the `@@gen_query_id@@` placeholder. Each occurrence of such placeholder will be replaced with a unique identifier consisting of the following underscore-separated components:

* Scenario ID
* Connection ID
* Query ID

*Please note these components don't reflect the logical scenario organization (i.e. units). It is, therefore, not possible to grep for all queries from a single unit (unless the unit only contains one connection).*

With this information, you can grep server logs to identify each individual query, all queries from a single scenario or all queries within a single database connection.

Here is an example unit configuration:

```yaml
---
unit_name: unit1
description: Example of a successful unit
query: "select /*+label(@@gen_query_id@@) */ 1"
uri: "jdbc:h2:mem://localhost"
driver_class: org.h2.Driver
username: sa
password: ""
parallel_connections: 2
repeats: 5
connection_timeout: 500
query_timeout: 500
```

To illustrate how can such labeling can be useful, let's consider debugging and profiling [query labels](https://my.vertica.com/docs/7.1.x/HTML/Content/Authoring/AdministratorsGuide/Profiling/HowToLabelQueriesForProfiling.htm) of HP Vertica. Labels provide an easy way how to pair the test query executions with the database server log entries.

### Error handling

Various kinds of errors can occur during the scenario run, two most important categories of errors are _connection initialization errors_ and _query errors_.

When a connection initialization fails (either due to an exception or a timeout), _dbstress_ does not proceed to the _query execution phase_ and terminates immediately.

Query errors, on the other hand, are nothing special, failed queries are simply reported as failures and unit run proceeds to the next iteration.

Information about both _connection initialization errors_ and _query errors_, along with the reasons (i.e. exception messages) are contained in the application log.

The following list summarizes the various exit status codes:

* 0: Success, output CSV generated. Individual queries still may have failed, though
* 1: Error, incorrect command line arguments
* 2: Error parsing the configuration YAML file
* 3: Some database connections could not be initialised
* 4: Scenario timeout
* 10+: Unexpected application errors, should be reported as bugs

## Results

When a scenario run finishes, it creates file `summary.${timestamp}.csv` in the output directory.

The CSV summary file contains statistical values (min, max, mean, median and standard deviation) calculated from all/successful/failed samples.

## Issues
If you have any problem with the application, find a bug or encounter an unexpected behaviour, please [create an issue](https://github.com/semberal/dbstress/issues/new) in the _dbstress_ [issue tracker](https://github.com/semberal/dbstress/issues) on GitHub.

## Building

You can build your own _dbstress_ distribution by cloning this repo and running `sbt universal:packageBin` for _zip_
package or `sbt universal:packageZipTarball` for a tarball.
