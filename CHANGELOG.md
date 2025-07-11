# Change Log
[//]: # (RDF Transform Version Control)

## 2.3.3 Released
This version is updates RDF output format processing. The preview and export processes were
changed from using a Model to a DatasetGraph to manage statement. This allows for Named Graph
use (using the BaseIRI). That change wrongly assumed all Jena languages could handle a DatasetGraph.
A Model is inherently a single Graph manager while a DatasetGraph manages multiple Graphs. A
language is either Graph-centric (Turle, NTriple, etc) or DatasetGraphs-centric (TriG, NQuad, etc.).
A Graph-centric language cannot process a DatasetGraph. This release corrects Graph-centric
languages to use a DatasetGraph's Union Graph.
This release also corrects reconstruction issues during application and extension loading. RDF
Transform now uses Jackson explicitly for Operation reconstruction.

## 2.3.2 Released
This version is updates the Namespace management. The Prefix, Namespace, Location, and
Location Type are store for each Namespace in the project. This expands on the older Prefix
and Namespace storage. It allows the Namespace management to recall from where it loaded the
Namespace. For URLs, they may differ from the Namespace IRI. For Files, the uploaded file is
stored in the RDF Transform cache in a project directory.

## 2.3.1 Released
This version is a minor update. The RDF Node dialog is updated with a resizer. Additionally,
the dialog's left column "Content" resizes with the dialog and adds scrollbars when needed. The
dialog and related left column Content are initialize to their minimum size when opened. This
fixes an issue when displaying a long "Content" (column names) lists where the bottom of the list
was not reachable--it extended down beyond the visible screen. This issue also made the "OK" and
"Cancel" buttons inaccessible. The dialog also allows widening to better view long column names.

## 2.3.0 Release
This version added the initial Context IRI support for Quad-enabled formats. The Preview screen
output is changed from Turtle to TriG. Fixed VocabularyImporter to import owl:ObjectProperty and
owl:DatatypeProperty. Changed hardcodes vocabulary queries to preference values. Added
CHANGELOG.md file. Update Copyright to 2025.

## 2.2.4 Release
This version corrects the Lucene ontology copy process from Global space to a Project and adds
CSRF protection to exports.

## 2.2.3 Release
This version updates dependencies and fixes the ontology loading process. It also mitigates facet
processing issues where RDF Transform fails to injects command processing.

## 2.2.2 Release
This version is a minor update for the copyright notice and fixes the arrow start and end images
removed from the OpenRefine main project.

## 2.2.1 Release
This version updated the LICENSE.txt and adds license text to all revelant project files.
It also fixes dialog loading issues due to the added license text.
It fixes browser loading issues due to unnamed static declarations.

## 2.2.0 Release
This version updated the Exporters for use with the latest Apache Jena library used by
OpenRefine. This requires OpenRefine 3.6 or better. The Export menu has been recoded for Stream
and Pretty formats. The exports have been optimized to process the RDF statements in critical
sections. Resizing the display tabs have been updated to correctly calculate the initial sizes.
The Transform tab has been updated for proper Prefix vertical alignment. Export server calls
have been normalized to use the Jena library RDFFormat names to simplify Command Line use for
batch processing transforms. Server results returned to the client have been normalized to use
common code responses. Preview controls have been updated to better select between Stream and
Pretty mode. Corrections for some outling UTF-8 encoding issues are included. A Japanese
translation file was added and resulted in an OpenRefine 3.7 update to rename Japanese
translation files from ".jp" to ".ja". POM file updates have reduced the RDF Transform
extension file to 8.9 MiB!

## 2.1.1 Beta Release
This version corrects Literal processing for record-based literals at the Tier 2 level and below.
Literals not directly connected to a root node would not process properly when in OpenRefine
"record" mode. These literals would not be present in the preview or in export files. Other
enhancements include preview controls for setting 1) the sample size for records or rows to
process and 2) the preview output type based on a Jena Stream or Pretty format, new
RDFTransform.previewStream preference as default for preview output type, updated the preview
expression dialog, expanded the exports with Pretty Formats, updated the Lucene version, code
management, and log and debug enhancements.

## 2.1.0 Beta Release
This version correct the Namespace processing by Web and File Import. The Web Import controls
faulty content negotiation for the non-existent XMLSchema ontology. The File Import corrects
the client POST processing for form content including file streams. Other enhancements include
further corrects for the "controller" initialization code, removing Binary RDF processing and
adding RDFThrift processing to match the Jena change, an RDFTransform.debugJSON preference to
manage the transform template output separate from other debugging, code management, log and
debug enhancements, and website example updates.

## 2.0.5 Alpha Release
This version has replaced the RDF4J library with the Jena library resulting in a 2/3rds reduction
of the release file size. The "controller" initialization code has been rewritten as Java code in
the InitializationCommand class. Other enhancements include Literal processing corrections, new
export selections to match the Jena change, code management, log and debug enhancements, and
website example updates. The version is back to an Alpha state due to the Jena overhaul.

## 2.0.4 Beta Release
This version includes BaseIRI export checking, UI image changes, code management, log and debug
enhancements, and website example updates.

## 2.0.3 Alpha Release
This version includes release version control updates and adds expression management when switching
between RDF Node types (index, column, constant).

## 2.0.2 Alpha Release
This version includes preparation for OpenRefine 4.0 project namespace changes and fixes several
Blank Node issues.

## 2.0.1 Alpha Release
This version includes dialog tab fixes for the Italian translation and further updates dialog tabs
to use i18n translation strings.

## 2.0.0 Alpha Release
This version is a complete overhaul of the venerable RDF extension.  The re-coding and structure
includes many new features and enhancements.
