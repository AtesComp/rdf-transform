![Build Status](https://github.com/AtesComp/rdf-transform/workflows/Java%20CI%20with%20Maven/badge.svg)

This project adds a graphical user interface (GUI) for transforming data of OpenRefine projects to RDF format. The transform is based on mapping the data to a template graph using the GUI.

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
