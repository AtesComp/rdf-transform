# RDF Transform #
![Build Status](https://github.com/AtesComp/rdf-transform/workflows/Java%20CI%20with%20Maven/badge.svg)

This project adds a graphical user interface (GUI) for transforming OpenRefine project data to RDF format. The transform is based on mapping the data to a template graph using the GUI.

This project is based on the venerable "RDF Extension" ([grefine-rdf-extension](https://github.com/stkenny/grefine-rdf-extension)). However, it has been throughly rewritten to incorporate the latest Java and JavaScript technologies and processing enhancements.

* JavaScripts have been updated to use "classified" coding
* Loops use iterators
* Cleaned UI elements
* Resizable dialogs
* Export capabilities have been expanded to all known RDF4J formats
* Properly recognize the Row verses Record data setting and processing (row / record visitor)
* Properly parse IRIs for valid structure, absolute and relative, using a base IRI as needed
* Added two GREL functions:
  * "toIRIString" - transforms and properly validates a string as an IRI component
  * "toStrippedLiteral" - end trims a string with all known Unicode whitespace and non-breaking space characters
* Properly process an IRIs Condensed IRI Expression (CIRIE) for output / export
* Scaled buffer allocations (based on data size) to speed exports
* Template graphs are exportable / importable (like OntoRefine) between different (but similar data structure) projects
* General code cleanup and commenting throughout
* Added "RFTransform.verbose" preference (see OpenRefine preferences) to aid checking and debugging
  * Defaults to "verbose" preference (maybe OpenRefine will use it as a base preference) or 0
  * 0 (or missing preference) == no verbosity and unknown, uncaught errors (stack traces, of course)
  * 1 == basic functional information and all unknown, caught errors
  * 2 == additional info and warnings on well-known issues: functional exits, permissibly missing data, etc
  * 3 == detailed info on functional minutiae and warnings on missing, but desired, data
  * 4 == controlled error catching stack traces, RDF preview statements, and other highly anal minutiae
* Added "RFTransform.exportLimit" preference (see OpenRefine preferences) to limit the statement buffer and optimize output
  * The statement buffer (i.e., an internal memory RDF repository) stores created statements from the data
  * The old system created one statement in the buffer, then flushed the buffer to disk--very inefficient
  * The new system holds many statement before before flushing to disk.
  * This buffer can become large if the data is large and produces many statements, so it is somewhat optimized:
    * Given a default statement size of 100 bytes, the default buffer is limited to 1024 * 1024 * 1024 / 100 = 1GiB / 100 = 10737418 statements
    * The 100 byte statement size is likely large as the average statement size is likely smaller
    * Regardless, this keeps memory usage to about 1GiB or less and a user can set the preference to optimize for a given memory footprint and data size
  * Then, the buffered statements optimize the creation and flush processes to speed the disk write
* Added "RFTransform.debug" preference (see OpenRefine preferences) to aid debugging
  * Controls the output of specifically marked "DEBUG" messages

NOTE: To streamline RDF Transform, the RDF reconcile functionality has been removed from this project.  The reconcile code is intended to be recreated as a separate project.  Additionally, OpenRefine has native reconciliation services. 

## DOWNLOAD ##

### Latest Release ###

[RDF Transform v2.0.0](https://github.com/AtesComp/rdf-transform/releases/download/v2.0.0/rdf-transform-2.0.0.zip)

### Previous Releases - for earlier versions of OpenRefine ###

None

## INSTALL ##

### Prerequisites ##

You need to have Java and OpenRefine installed on your machine.

* Java 11+
* OpenRefine 3.5+

### From Compiled Release ###
<ol>
<li>If it does not exist, create a folder named <b>extensions</b> under your user workspace directory for OpenRefine. The workspace should be located in the following places depending on your operating system (see the <a href=https://github.com/OpenRefine/OpenRefine/wiki/FAQ-Where-Is-Data-Stored>OpenRefine FAQ</a> for more details):
  <ul>
  <li>Linux ~/.local/share/OpenRefine</li>
  <li>Windows C:/Documents and Settings/&lt;user&gt;/Application Data/OpenRefine OR C:/Documents and Settings/&lt;user&gt;/Local Settings/Application Data/OpenRefine</li>
  <li>Mac OSX ~/Library/Application Support/OpenRefine</li>
  </ul>
As an alternative (but not recommended), use the OpenRefine application's extensions directory instead.</li>
<li>Unzip the downloaded release (ensuring it is a <code>rdf-transform-x.x.x.zip</code> and <b>not</b> a source code <code>.zip</code> or <code>.tar.gz</code>) in the <b>extensions</b> folder (within the directory of step 1). This will create an <b>rdf-transform</b> directory containg the extension.</li>
<li>Start (or restart) OpenRefine (see the <a href=https://github.com/OpenRefine/OpenRefine/wiki/Installation-Instructions#release-version>OpenRefine User Documentation</a>)</li>
</ol>

NOTE: It is recommended that you have an active internet connection when using the extension as it can download ontologies from specified namespaces (such as rdf, rdfs, owl and foaf). You can (re)add namespaces and specify whether to download the ontology (or not) from the namespace declaration URL. If you must run OpenRefine from an offline location, you can copy the ontologies to files in your offline space and use the "from file" feature to load the ontologies.

### From Source - Build ###
1. From some top level development directory, create a local repository for this RDF Transform extension:
    * From the top level development directory where you want the /rdf-transform sub-directory:
      * Clone the extension:
        - `git clone https://github.com/AtesComp/rdf-transform`
    * Alternatively, to update an existing clone, in the /rdf-transform directory:
      * To update:
        - `git pull`
      * Change directories up one level:
        - `cd ..`
2. Prepare the OpenRefine jar file for rdf-transform:
    * From the same top level development directory, create a local repository for OpenRefine:
      * Clone OpenRefine:
        - `git clone https://github.com/OpenRefine/OpenRefine`
    * Create the OpenRefine jar:
      * Change directories to OpenRefine:
        - `cd OpenRefine`
      * Clean the dev environment:
        - `./refine clean`
      * Build OpenRefine:
        - `./refine build`
      * Build the OpenRefine jar:
        - `./refine dist 3.6-SNAPSHOT` or latest version
      * Among many other things, this builds the needed jar file: `OpenRefine/main/target/openrefine-main.jar`
3. Process the RDF Transform extension:
    * Change directories to the RDF Transform extension:
      - `cd ../rdf-transform`
    * Adjust the `openrefine-shim-pom.xml` file to use the proper OpenRefine version - in this example: `3.6-SNAPSHOT`
    * Adjust the `pom.xml` file to use the proper OpenRefine version - in this example: `3.6-SNAPSHOT`
    * Put the OpenRefine in the Maven library for RDF Transform:
      - `mvn install:install-file -Dfile=../../OpenRefine//main/target/openrefine-main.jar -DpomFile=openrefine-shim-pom.xml`
    * Clean and compile the extension dev environment:
      - `mvn clean compile`
    * Assemble the extension:
      - `mvn assembly:single`
    * Copy and unzip the `target/rdf-transform-x.x.x.zip` file in the **extensions** directory as documented in <a name="From Compiled Release">From Compiled Release</a> above

If you have previously installed the extension you will need to replace it in the **extensions** directory with the newly built version, e.g., delete rdf-transform directory and unzip file.

### Issues ###
None, currently.

## Documentation ##
* [Documentation Wiki](https://github.com/AtesComp/rdf-transform/wiki)
