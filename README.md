# RDF Transform
<div align="left">
  <a target="_blank" rel="noopener noreferrer" href="https://github.com/AtesComp/rdf-transform/actions/workflows/maven.yml">
    <img align="left" width="200px" src="https://github.com/AtesComp/rdf-transform/workflows/Java%20CI%20with%20Maven/badge.svg" />
  </a>
  <img align="left" />
</div>
<details><summary>Build Note</summary>On failed builds, Maven repositories may need to be reset. Review Actions tab for issues. If needed, run the "Maven Reset Dependencies" workflow.</details>

## Introduction
This project uses a graphical user interface (GUI) for transforming OpenRefine project data to RDF-based formats. The transform maps the data with a template graph designed using the GUI.

RDF Transform is based on the venerable "RDF Extension" ([grefine-rdf-extension](https://github.com/stkenny/grefine-rdf-extension)). However, it has been thoroughly rewritten to incorporate the newer Java and JavaScript technologies, techniques, and processing enhancements.

## Documentation
See the [wiki](https://github.com/AtesComp/rdf-transform/wiki) for more information.

![](website/images/rdf-transform_annotated.png)

## Download
<!-- RDF Transform Version Control -->

### Latest Release
[RDF Transform v2.1.1-beta](https://github.com/AtesComp/rdf-transform/releases/download/v2.1.1-beta/rdf-transform-2.1.1.zip)

### Previous Releases
[RDF Transform v2.1.0-beta](https://github.com/AtesComp/rdf-transform/releases/download/v2.1.0-beta/rdf-transform-2.1.0.zip)<br />
[RDF Transform v2.0.5-alpha](https://github.com/AtesComp/rdf-transform/releases/download/v2.0.5-alpha/rdf-transform-2.0.5.zip)<br />
[RDF Transform v2.0.4-beta](https://github.com/AtesComp/rdf-transform/releases/download/v2.0.4-beta/rdf-transform-2.0.4.zip)<br />
[RDF Transform v2.0.3-alpha](https://github.com/AtesComp/rdf-transform/releases/download/v2.0.3-alpha/rdf-transform-2.0.3.zip)<br />
[RDF Transform v2.0.2-alpha](https://github.com/AtesComp/rdf-transform/releases/download/v2.0.2-alpha/rdf-transform-2.0.2.zip)<br />
[RDF Transform v2.0.1-alpha](https://github.com/AtesComp/rdf-transform/releases/download/v2.0.1-alpha/rdf-transform-2.0.1.zip)<br />
[RDF Transform v2.0.0-alpha](https://github.com/AtesComp/rdf-transform/releases/download/v2.0.0-alpha/rdf-transform-2.0.0.zip)

## Install
See the [Install page on the wiki](https://github.com/AtesComp/rdf-transform/wiki/Install) for more information.

## Issues
***General interaction issue with OpenRefine versions, Web Browsers, OSes, etc., not specifically code related.***

**NOTE**: It is recommended that you have an active Internet connection when using the extension as it can download ontologies from specified namespaces (such as rdf, rdfs, owl and foaf). You can (re)add namespaces and specify whether to download the ontology (or not) from the namespace declaration URL. If you must run OpenRefine from an offline location, you can copy the ontologies to files in your offline space and use the "from file" feature to load the ontologies.

### OpenRefine
As an extension, RDF Transform runs under the control of OpenRefine and its JVM. As such, the libraries included with OpenRefine override any of the same libraries included with the extension. This limits the extension to OpenRefine's version of those library functions and features.

### OSes
See the [Install page on the wiki](https://github.com/AtesComp/rdf-transform/wiki/Install) for related information.

#### Linux
RDF Transform has been tested against OpenRefine 3.5.2 and the rolling 3.6-SNAPSHOT on a modern Debian-based OS (Ubuntu derivative) using Chrome. No system related issue were found under these conditions.

#### Windows
Test runs on MS Windows 10 have indicated the JVM opertate slightly different than on Linux. The MS Windows version tends to be more sensitive to certain statements.
1. The version of Simile Butterfly that processes the limited server-side JavaScript engine can fail on unused declarative statements such as "importPackage()". If the package is not found, Windows systems may silently fail to run any following statements whereas Linux systems will continue. To mitigate against server-side JavaScript issues, all possible server-side JavaScript code has been migrated to Java.
2. The JVM relies on OS specific services to process network connections. It may process web-based content negotiation differently on a particular OS. On Windows, if the URL does not produce the expected response, negotiation and the related response processing may lock the process for an unreasonably long time whereas Linux may fail safe and quickly. To mitigate against web content negotiation issues, a Faulty Content Negotiation processor is used identify known fault intolerant processing.  As faults become known, they are added to the processor.

#### Mac
In all instances, the MacOS versions of OpenRefine are currently bundled with Java 8 JRE. Since RDF Transform requires Java 11 to 17, the bundled Java should be overridden with:
1. A later Java install, preferably Java 11 JDK or Java 17 JDK
   * Java installs later than 8 do not have a separate JRE install
2. Setting the JAVA_HOME env variable to the later Java install directory

### Reporting
Please report any problem using RDF Transform to the code repository's [Issues](https://github.com/AtesComp/rdf-transform/issues).
