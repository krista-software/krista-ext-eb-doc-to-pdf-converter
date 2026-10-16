# Overview

EB Doc To Pdf Converter converts a Word document into PDF. Unlike most Krista extensions, it does not
call out to a third-party API - the conversion happens entirely in-process, inside the extension, using
the open-source Apache POI and PDFBox libraries. Use it anywhere a Krista workflow or agent needs to
turn a Word document into a shareable, standardized PDF.

## Use cases

- **Convert a Word document to PDF** - attach a `.doc` or `.docx` file and get back a PDF file with the
  same text content, basic emphasis (bold/italic), paragraph alignment, and tables.

## What you can do

The extension exposes **1 catalog request** in the Document Conversion area, plus the standard Setup
area (Test Connection, Health Check) - see [Supported Requests](pages/supported-requests.md) for the
full list, or the sidebar for each request's detailed page.

## Before you start

1. Nothing to configure - see [Authentication](pages/Authentication.md) and
   [Configuration](pages/ExtensionConfiguration.md) for why.
2. Run **Test Connection** (Setup area) to confirm the bundled conversion libraries are working in your
   environment.
3. Call **Convert Document to PDF** with a Word file attached.
