# RDF Transform #
![Build Status](https://github.com/AtesComp/rdf-transform/workflows/Java%20CI%20with%20Maven/badge.svg)

This project adds a graphical user interface (GUI) for transforming OpenRefine project data to RDF-based formats. The transform maps the data with a template graph using the GUI.

RDF Transform is based on the venerable "RDF Extension" ([grefine-rdf-extension](https://github.com/stkenny/grefine-rdf-extension)). However, it has been throughly rewritten to incorporate the newer Java and JavaScript technologies, techniques, and processing enhancements.

* Transform Tab:
  * RDF Node Editor: Added Prefix selection to the editor with Prefix and LocalPart management throughout the code
    * Preview Expression Editor:
      * Added two GREL functions:
        * toIRIString() - transforms and properly validates a string as an IRI component (replaces urlify())
        * toStrippedLiteral() - end trims a string with all known Unicode whitespace and non-breaking space characters
  * Property Editor: Initializes with the existing property in the textbox instead of blank text
  * Added universal Expand (>) and Collapse (v) control on all nodes and properties
  * Added universal delete (x) control on all nodes and properties
* Preview Tab:
  * All changes made in the Transform tab are reserved until the user switches to the Preview tab
  * (FUTURE) Editable sample record / row count for preview
* User Inferface:
  * Cleaned and refactored UI elements (but it still looks mostly like the original RDF extension)
  * Resizable dialogs
  * Uses a template system that is exportable / importable (like OntoRefine) for use between different (but similar data structure) projects
    * The template is stored as a JSON formatted structure
    * The structure, while similar to OntoRefine, uniformly normalizes key names and substructures
    * The same structure is used within the native OpenRefine data store for project and change management
* Code:
  * JavaScript code has been updated to use "classified" coding
  * Loops use iterators whenever possible
  * RDF Export capabilities have been expanded to all known RDF4J formats
  * Properly recognize the Row verses Record parameters and processing (row and record visitors)
  * Properly parse IRIs for valid structure, prefix and local part, absolute and relative, using the base IRI as needed
  * Properly process an IRI's Condensed IRI Expression (CIRIE, a.k.a., Prefix + LocalPart) for output / export
  * Reserve flushing of scaled statements buffers to speed exports (user definable--see "RDFTransform.exportLimit" below)
  * The "namespaces" and "predefined_vocabs" support files are processed using general whitespace separation (not strictly tab delimited)
  * The code differentiates between "namespace" (Prefix and IRI) versus "prefix" use
  * General cleanup and verbose commenting throughout the code
* Preferences: Three preferences were added to manage server output
  * "RDFTransform.verbose" preference (see OpenRefine preferences) aids with process feedback and debugging
    * A general "verbose" preference is rcognized as a default (HINT: OpenRefine might use it as a base preference)
    * 0 == no verbosity and unknown, uncaught errors (stack traces, of course)
    * 1 == basic functional information and all unknown, caught errors
    * 2 == additional info and warnings on well-known issues: functional exits, permissibly missing data, etc
    * 3 == detailed info on functional minutiae and warnings on missing, but desired, data
    * 4 == controlled error catching stack traces, RDF preview statements, and other highly anal minutiae
    * A missing verbose preference defaults to 0
  * "RDFTransform.exportLimit" preference (see OpenRefine preferences) limits the statement buffer and optimizes output
    * The statement buffer (i.e., an internal memory RDF repository) stores statements created from the data
    * The old system created one statement in the buffer, then flushed the buffer to disk--very inefficient
    * The new system holds many statement before before flushing to disk.
    * This buffer can become large if the data is large and produces many statements, so it is somewhat optimized:
      * Given a default statement size of 100 bytes, the default buffer is limited to 1024 * 1024 * 1024 / 100 = 1GiB / 100 = 10737418 statements
      * The 100 byte statement size is likely large as the average statement size is likely smaller
      * Regardless, this keeps memory usage to about 1GiB or less and a user can set the preference to optimize for a given memory footprint and data size
    * Then, the buffered statements optimize the creation and flush processes to speed the disk write
    * A future enhancement may examine the project data size and system memory to determine an optimize buffer size and allocations
  * Added "RDFTransform.debug" preference (see OpenRefine preferences) to aid debugging
    * Controls the output of specifically marked "DEBUG" messages
    * Includes many verbose output messages as well

NOTE: To streamline RDF Transform, the RDF reconcile functionality has been removed from this project.  The reconcile code is intended to be recreated as a separate project.

## RDF Transform Template ##

The structure of the RDF Transform template is stored in a uniform JSON format.  The following example illustrates the template.
```
{
   "baseIRI" : "some://Example/",
   "extension" : "RDFTransform",
   "namespaces" : {
      "foaf" : "http://xmlns.com/foaf/0.1/",
      "myp" : "some://Example/namspace/",
      "owl" : "http://www.w3.org/2002/07/owl#",
      "rdf" : "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "rdfs" : "http://www.w3.org/2000/01/rdf-schema#",
      "vcard" : "http://www.w3.org/2006/vcard/ns#"
   },
   "subjectMappings" : [
      {
         "expression" : {
            "code" : "toIRIString(row.record.index)",
            "language" : "grel"
         },
         "prefix" : "myp",
         "propertyMappings" : [
            {
               "objectMappings" : [
                  {
                     "expression" : {
                        "code" : "toIRIString(toStrippedLiteral(value))",
                        "language" : "grel"
                     },
                     "prefix" : "myp",
                     "valueSource" : {
                        "columnName" : "name/family",
                        "source" : "column"
                     },
                     "valueType" : {
                        "type" : "iri"
                     }
                  }
               ],
               "prefix" : "myp",
               "valueSource" : {
                  "constant" : "familyName",
                  "source" : "constant"
               }
            },
            {
               "objectMappings" : [
                  {
                     "valueSource" : {
                        "columnName" : "timestamp",
                        "source" : "column"
                     },
                     "valueType" : {
                        "datatype" : {
                           "prefix" : "xsd",
                           "valueSource" : {
                              "constant" : "dateTime",
                              "source" : "constant"
                           }
                        },
                        "type" : "datatype_literal"
                     }
                  }
               ],
               "prefix" : "myp",
               "valueSource" : {
                  "constant" : "collectedOn",
                  "source" : "constant"
               }
            },
            {
               "objectMappings" : [
                  {
                     "valueSource" : {
                        "columnName" : "notes/comments",
                        "source" : "column"
                     },
                     "valueType" : {
                        "language" : "en",
                        "type" : "language_literal"
                     }
                  }
               ],
               "prefix" : "mpy",
               "valueSource" : {
                  "constant" : "notes",
                  "source" : "constant"
               }
            }
         ],
         "typeMappings" : [
            {
               "prefix" : "ioc",
               "valueSource" : {
                  "constant" : "Indicator",
                  "source" : "constant"
               }
            }
         ],
         "valueSource" : {
            "source" : "record_id"
         }
      }
   ],
   "version" : "2.0.0"
}

```

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

The compiled release file is the "Easy Button" to get RDF Transform installed as an extension to OpenRefine.  Follow these instructions to get it running.

<ol>
<li>If it does not exist, create a folder named <b>extensions</b> under your user workspace directory for OpenRefine. The workspace should be located in the following places depending on your operating system (see the <a href=https://github.com/OpenRefine/OpenRefine/wiki/FAQ-Where-Is-Data-Stored>OpenRefine FAQ</a> for more details):
  <ul>
  <li>Linux <code>~/.local/share/OpenRefine</code></li>
  <li>Windows <code>C:/Documents and Settings/&lt;user&gt;/Application Data/OpenRefine</code> OR <code>C:/Documents and Settings/&lt;user&gt;/Local Settings/Application Data/OpenRefine</code></li>
  <li>Mac OSX <code>~/Library/Application Support/OpenRefine</code></li>
  </ul>
As an alternative (but not recommended), use the OpenRefine application's extensions directory instead.</li>
<li>Unzip the downloaded release (ensuring it is a <code>rdf-transform-x.x.x.zip</code> and <b>not</b> a source code <code>.zip</code> or <code>.tar.gz</code>) in the <b>extensions</b> folder (within the directory of step 1). This will create an <b>rdf-transform</b> directory containg the extension.</li>
<li>Start (or restart) OpenRefine (see the <a href=https://github.com/OpenRefine/OpenRefine/wiki/Installation-Instructions#release-version>OpenRefine User Documentation</a>)</li>
</ol>

NOTE: It is recommended that you have an active internet connection when using the extension as it can download ontologies from specified namespaces (such as rdf, rdfs, owl and foaf). You can (re)add namespaces and specify whether to download the ontology (or not) from the namespace declaration URL. If you must run OpenRefine from an offline location, you can copy the ontologies to files in your offline space and use the "from file" feature to load the ontologies.

### From Source - Build ###

Source code...for those of you who want more depth...to ply the inner workings of OpenRefine.  You still need to install it to test and debug any modifications, so here are those complete instructions.

NOTE: If you have previously installed the extension, you will need to replace it in the **extensions** directory with the newly built version, e.g., delete rdf-transform directory in the **extensions** directory and unzip the new file.

#### TLDR; ####

Short:
```
git clone https://github.com/AtesComp/rdf-transform
cd rdf-transform
mvn clean compile
mvn assembly:single
rm -rf ~/.local/share/openrefine/extensions/rdf-transform* 
unzip target/rdf-transform-3.6-SNAPSHOT.zip -d ~/.local/share/openrefine/extensions
~/path/to/openrefine/refine
```
Long:
```
git clone https://github.com/AtesComp/rdf-transform
git clone https://github.com/OpenRefine/OpenRefine
cd OpenRefine
./refine clean
./refine build
./refine dist 3.6-SNAPSHOT
cd ../rdf-transform
mvn install:install-file -Dfile=../OpenRefine/main/target/openrefine-main.jar -DpomFile=openrefine-shim-pom.xml -DcreateChecksum=true -DlocalRepositoryPath=./project-repository
mvn clean compile
mvn assembly:single
rm -rf ~/.local/share/openrefine/extensions/rdf-transform* 
unzip target/rdf-transform-3.6-SNAPSHOT.zip -d ~/.local/share/openrefine/extensions
cd ../OpenRefine
./refine
```

#### Short Steps ####

A local project repository (see the "project-repository" directory) contains an OpenRefine jar file ready for use by the maven compile process.  If you want or need to compile OpenRefine, see the [Long Steps](#long-steps) below to create the OpenRefine jar file.

1. From some top level development directory, create a local repository for this RDF Transform extension:
    * Clone the extension at the top level development directory where you want the /rdf-transform sub-directory:
      - `git clone https://github.com/AtesComp/rdf-transform`
2. Compile the RDF Transform extension:
    * Change directories to the RDF Transform extension:
      - `cd rdf-transform`
    * Clean and compile the extension's dev environment:
      - `mvn clean compile`
    * Assemble the extension:
      - `mvn assembly:single`
    * Copy and unzip the `target/rdf-transform-x.x.x.zip` file in the **extensions** directory as documented in [From Compiled Release](#from-compiled-release) above

#### Long Steps ####

Sometime you just have to do everything yourself.  If you want or need to compile OpenRefine, then you'll probably want to create the jar file for RDF Transform to match.  From the **Short Steps**, you'll notice these instructions have two inserted steps betwwen 1 and 2.

1. From some top level development directory, create a local repository for this RDF Transform extension:
    * Clone the extension at the top level development directory where you want the /rdf-transform sub-directory:
      - `git clone https://github.com/AtesComp/rdf-transform`
    * Alternatively, to update an existing clone, in the /rdf-transform directory:
      * Change directories to the RDF Transform development directory:
        - `cd rdf-transform`
      * Update the code:
        - `git pull` (or `git fetch --all; git reset --hard; git pull` for a forced refresh)
      * Change directories up one level:
        - `cd ..`
2. Prepare the OpenRefine jar file:
    * Clone OpenRefine from the same top level development directory to create a local repository:
      - `git clone https://github.com/OpenRefine/OpenRefine`
    * Create the OpenRefine jar:
      * Change directories to OpenRefine:
        - `cd OpenRefine`
      * Clean OpenRefine's dev environment:
        - `./refine clean`
      * Build OpenRefine:
        - `./refine build`
      * Build the OpenRefine jar:
        - `./refine dist 3.6-SNAPSHOT` (or use the latest version id)
      * Among many other things, this builds the needed jar file: `OpenRefine/main/target/openrefine-main.jar`
      * Change directories up one level:
        - `cd ..`
3. Process the OpenRefine jar file for the RDF Transform extension:
    * Change directories to the RDF Transform extension:
      - `cd rdf-transform`
    * Adjust the `openrefine-shim-pom.xml` file to use the proper OpenRefine version id - in this example: `3.6-SNAPSHOT`
    * Adjust the `pom.xml` file to use the proper OpenRefine version id - in this example: `3.6-SNAPSHOT`
    * Install the OpenRefine jar in the Maven library for RDF Transform:
      - `mvn install:install-file -Dfile=../OpenRefine/main/target/openrefine-main.jar -DpomFile=openrefine-shim-pom.xml -DcreateChecksum=true -DlocalRepositoryPath=./project-repository`
4. Compile the RDF Transform extension:
    * Clean and compile the extension's dev environment:
      - `mvn clean compile`
    * Assemble the extension:
      - `mvn assembly:single`
    * Copy and unzip the `target/rdf-transform-x.x.x.zip` file in the **extensions** directory as documented in [From Compiled Release](#from-compiled-release) above

### Issues ###
None, currently.

### Developer Testing and Debugging ###
Here are some tips on testing and debugging...

#### Java ####
Tinker with the "RDFTransform.verbose" and "RDFTransform.debug" configurations settings for RDT Transform to get plenty 'o feedback.  Generally, setting the debug configuration to "true" will do the trick.

#### Javascript ####
Use the built-in Developer Tools in your perferred browser.

#### HTML Web Pages ####
If you need to test or debug the HTML web pages, you'll need to set up a "server".

Change directories to project's website source: 
`cd rdf-transform/website`

Start the simple python HTTP server:
`python3 -m http.server`

Load the "index.html" file in a browser. You may need to adjust your browser security setting to allow LocalHost access to local files.  That issues is beyond this documentation.
`http://localhost:8000`

## Documentation ##
* [Documentation Wiki](https://github.com/AtesComp/rdf-transform/wiki)
