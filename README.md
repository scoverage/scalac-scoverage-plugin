# scalac-scoverage-plugin

![build](https://github.com/scoverage/scalac-scoverage-plugin/workflows/build/badge.svg)
[![Gitter](https://img.shields.io/gitter/room/scoverage/scoverage.svg)](https://gitter.im/scoverage/scoverage)
[![Maven Central](https://img.shields.io/maven-central/v/org.scoverage/scalac-scoverage-plugin_2.10.svg?label=latest%20release%20for%202.10)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scalac-scoverage-plugin_2.10%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.scoverage/scalac-scoverage-plugin_2.11.12.svg?label=latest%202.11%20Scala%20support%20[2.11.12]%20and%20latest%20version)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scalac-scoverage-plugin_2.11.12%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.scoverage/scalac-scoverage-plugin_2.12.14.svg?label=latest%202.12%20Scala%20support%20[2.12.14]%20and%20latest%20version)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scalac-scoverage-plugin_2.12.14%22)
[![Maven Central](https://img.shields.io/maven-central/v/org.scoverage/scalac-scoverage-plugin_2.13.6.svg?label=latest%202.13%20Scala%20support%20[2.13.6]%20and%20version)](http://search.maven.org/#search|ga|1|g%3A%22org.scoverage%22%20AND%20a%3A%22scalac-scoverage-plugin_2.13.6%22)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

scoverage is a free Apache licensed code coverage tool for Scala that offers
statement and branch coverage.  scoverage is available for
[sbt](https://github.com/scoverage/sbt-scoverage),
[Maven](https://github.com/scoverage/scoverage-maven-plugin), and
[Gradle](https://github.com/scoverage/gradle-scoverage).

To see scoverage in action check out the
[samples](https://github.com/scoverage/scoverage-samples) project which shows
you covered and non-covered statements, along with an upload to coveralls.

![Screenshot of scoverage report html](misc/screenshot2.png)


### Statement Coverage

In traditional code coverage tools, line coverage has been the main metric. 
This is fine for languages such as Java which are very verbose and very rarely have more than one
statement per line, and more usually have one statement spread across multiple lines.

In powerful, expressive languages like Scala, quite often multiple statements, or even branches
are included on a single line, eg a very simple example:

```
val status = if (age < 18) "No beer" else "Beer for you"
```

If you had a unit test that ran through the value 18 you would get 100% line coverage
yet you only have 50% statement coverage.

Let's expand this example out to be multifacted, albeit somewhat contrived:

```
val status = if (religion == "Pentecostalist") "Beer forbidden" else if (age < 18) "Underage" else "Beer for you"
```

Now we would get 100% code coverage for passing in the values ("Buddhist", 34).

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
