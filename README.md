# scalac-scoverage-plugin

![build](https://github.com/scoverage/scalac-scoverage-plugin/workflows/build/badge.svg)
[![Gitter](https://img.shields.io/gitter/room/scoverage/scoverage.svg)](https://gitter.im/scoverage/scoverage)
[![Maven Central](https://img.shields.io/maven-central/v/org.scoverage/scalac-scoverage-plugin_2.11.12.svg?label=latest%202.11%20Scala%20support%20[2.11.12]%20and%20latest%20version)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scalac-scoverage-plugin_2.11.12%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.scoverage/scalac-scoverage-plugin_2.12.16.svg?label=2.12%20Scala%20support%20)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scalac-scoverage-plugin_2.12.16%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.scoverage/scalac-scoverage-plugin_2.13.8.svg?label=2.13%20Scala%20support%20)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scalac-scoverage-plugin_2.13.8%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.scoverage/scalac-scoverage-domain_3.svg?label=3%20Scala%20support%20)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scalac-scoverage-domain_3%22)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

scoverage is a free Apache licensed code coverage tool for Scala that offers
statement and branch coverage.  scoverage is available for
[sbt](https://github.com/scoverage/sbt-scoverage),
[Maven](https://github.com/scoverage/scoverage-maven-plugin), and
[Gradle](https://github.com/scoverage/gradle-scoverage).


**NOTE**: That this repository contains the Scala compiler plugin for Code coverage
in Scala 2 and other coverage utilities for generating reports. For Scala 3 code
coverage the [compiler](https://github.com/lampepfl/dotty) natively produces
code coverage output, but the reporting logic utilities are then shared with the
Scala 2 code coverage utilities in this repo.

![Screenshot of scoverage report html](misc/screenshot2.png)

### Statement Coverage

In traditional code coverage tools, line coverage has been the main metric. 
This is fine for languages such as Java which are very verbose and very rarely have more than one
statement per line, and more usually have one statement spread across multiple lines.

In powerful, expressive languages like Scala, quite often multiple statements, or even branches
are included on a single line, eg a very simple example:

```
val status = if (Color == Red) Stop else Go
```

If you had a unit test that ran through the Color Red you would get 100% line coverage
yet you only have 50% statement coverage.

Let's expand this example out to be multifacted, albeit somewhat contrived:

```
val status = if (Color == Red) Stop else if (Sign == Stop) Stop else Go
```

Now we would get 100% code coverage for passing in the values (Green, SpeedLimit).

That's why in scoverage we focus on statement coverage, and don't even include line coverage as a metric.
This is a paradigm shift that we hope will take hold.

### Branch Coverage

Branch coverage is very useful to ensure all code paths are covered. Scoverage produces branch coverage metrics
as a percentage of the total branches. Symbols that are deemed as branch statements are:

* If / else statements
* Match statements
* Partial function cases
* Try / catch / finally clauses

In this screenshot you can see the coverage HTML report that shows one branch of the if statement was not
executed during the test run. In addition two of the cases in the partial function were not executed.
![Screenshot of scoverage report html](misc/screenshot1.png)

### How to use

This project is the base library for instrumenting code via a scalac compiler plugin. To use scoverage in your
project you will need to use one of the build plugins:

* [scoverage-maven-plugin](https://github.com/scoverage/scoverage-maven-plugin)
* [sbt-scoverage](https://github.com/scoverage/sbt-scoverage)
* [gradle-scoverage](https://github.com/scoverage/gradle-scoverage)
* [sbt-coveralls](https://github.com/scoverage/sbt-coveralls)
* [mill-contrib-scoverage](https://www.lihaoyi.com/mill/page/contrib-modules.html#scoverage)
* Upload report to [Codecov](https://codecov.io): [Example Scala Repository](https://github.com/codecov/example-scala)
* Upload report to [Codacy](https://www.codacy.com/): [Documentation](https://support.codacy.com/hc/en-us/articles/207279819-Coverage)

Scoverage support is available for the following tools:

* [Sonar](https://github.com/RadoBuransky/sonar-scoverage-plugin)
* [Jenkins](https://github.com/jenkinsci/scoverage-plugin)

If you want to write a tool that uses this code coverage library then it is available on maven central.
Search for scalac-scoverage-plugin.

#### Excluding code from coverage stats

You can exclude whole classes or packages by name. Pass a semicolon separated
list of regexes to the 'excludedPackages' option.

For example:

    -P:scoverage:excludedPackages:.*\.utils\..*;.*\.SomeClass;org\.apache\..*

The regular expressions are matched against the fully qualified class name, and must match the entire string to take effect.

Any matched classes will not be instrumented or included in the coverage report.

You can also mark sections of code with comments like:

    // $COVERAGE-OFF$
    ...
    // $COVERAGE-ON$

Any code between two such comments will not be instrumented or included in the coverage report.

Further details are given in the plugin readme's.

### Release History

For a full release history please see the [releases
page](https://github.com/scoverage/scalac-scoverage-plugin/releases).
