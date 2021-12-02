![Build Status](https://github.com/AtesComp/rdf-transform/workflows/Java%20CI%20with%20Maven/badge.svg)

This project adds a graphical user interface (GUI) for transforming OpenRefine project data to RDF format. The transform is based on mapping the data to a template graph using the GUI.

This project is based on the venerable "RDF Extension" ([grefine-rdf-extension](https://github.com/stkenny/grefine-rdf-extension)).
However, it has been throughly rewritten to incorporate the latest Java and JavaScript technologies and processing enhancements:
  * JavaScripts have been updated to use "classified" coding
  * Loops use iterators
  * Cleaned UI elements
  * Resizable dialogs
  * Export capabilities have been expanded to all known RDF4J formats
  * Properly recognize the Row verses Record data setting and processing (row / record visitor)
  * Properly parse IRIs for valid structure, absolute and relative, using a base IRI as needed
  * Two GREL functions:
    * "forIRI" - transforms and properly validates string as an IRI
    * "toStrippedLiteral" - end trims string with all known Unicode whitespace and non-breaking space characters
  * Properly process Condensed IRI Expressions (CIRIEs) for export
  * Scaled buffer allocations (based on data size) to speed exports
  * Template graphs are exportable / importable (like OntoRefine) between different (but similar data structure) projects
  * General code cleanup and commenting throughout
  * Added "RFTransform/verbose" preference (see OpenRefine preferences) to aid checking and debugging
    * Defaults to "verbose" preference (maybe OpenRefine will use it as a base preference) or 0
    * 0 (or missing preference) == no verbosity and unknown, uncaught errors (stack traces, of course)
    * 1 == basic functional information and all unknown, caught errors
    * 2 == additional info and warnings on well-known issues: functional exits, permissibly missing data, etc
    * 3 == detailed info on functional minutiae and warnings on missing, but desired, data
    * 4 == controlled error catching stack traces, RDF preview statements, and other highly anal minutiae
  * Added "RFTransform/exportLimit" preference (see OpenRefine preferences) to limit the statement buffer and optimize output
    * The statement buffer (i.e., an internal memory RDF repository) stores created statements from the data
    * The old system created one statement in the buffer, then flushed the buffer to disk--very inefficient
    * The new system holds many statement before before flushing to disk.
    * This buffer can become large if the data is large and produces many statements, so it is somewhat optimized:
      * Given a default statement size of 100 bytes, the default buffer is limited to 1024 * 1024 * 1024 / 100 = 1GiB / 100 = 10737418 statements
      * The 100 byte statement size is likely large as the average statement size is likely smaller.
      * Regardless, this keeps memory usage to about 1GiB or less and a user can set the preference to optimize for a given memory footprint and data size.
    * Then, the buffered statements optimize the creation and flush processes to speed the disk write.

NOTE: To streamline RDF Transform, the RDF reconcile functionality has been removed from this project.  The reconcile code is intended to be recreated as a separate project.  Additionally, OpenRefine has native reconciliation services. 

## DOWNLOAD

### Latest release

[RDF Transform v2.0.0](https://github.com/AtesComp/rdf-transform/releases/download/v2.0.0/rdf-transform-2.0.0.zip)

### Previous releases for earlier versions of OpenRefine

None

## INSTALL

### Prerequisites

You need to have Java and OpenRefine installed on your machine.
  * Java 11+
  * OpenRefine 3.5+

### From compiled release

1. If it does not exist, create a folder named **extensions/rdf-transform** under your user workspace directory for OpenRefine. The workspace should be located in the following places depending on your operating system (see the [OpenRefine FAQ](https://github.com/OpenRefine/OpenRefine/wiki/FAQ-Where-Is-Data-Stored) for more details):
    * Linux ~/.local/share/OpenRefine
    * Windows C:/Documents and Settings/<user>/Application Data/OpenRefine OR C:/Documents and Settings/<user>/Local Settings/Application Data/OpenRefine
    * Mac OSX ~/Library/Application Support/OpenRefine
2. Unzip the downloaded release (ensuring it is a rdf-transform-x.x.x-*.zip and **not** a source code .zip or tar.gz) into the **extensions/rdf-transform** folder (within the diretory of step 1).
It is recommended that you have an active internet connection when using the extension as it can download ontologies from specified namespaces (such as rdf, rdfs, owl and foaf). You can (re)add namespaces and specify whether to download the ontology (or not) from the namespace declaration URL.
3. Start (or restart) OpenRefine (see the [OpenRefine user documentation](https://github.com/OpenRefine/OpenRefine/wiki/Installation-Instructions#release-version))

### To build from source
- Clone this extension repository to your local machine
- Checkout the main branch `git checkout main`
- Run `mvn clean compile` and `mvn assembly:single`
- Unpack the zip file created in the `target` directory to a sub-directory of the extensions folder of OpenRefine, e.g., `extensions/rdf-transform`

If you have previously installed the extension you will need to replace it in the OpenRefine extensions directory with the newly built version, e.g., the `module` subdirectory in the `extensions/rdf-transform` directory.

### Issues
None, currently.

## Documentation
* [Documentation Wiki](https://github.com/AtesComp/rdf-transform/wiki)
