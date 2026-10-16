# Convert Document to PDF

## Overview
Converts an uploaded Word document (.doc or .docx) to PDF and returns the generated PDF as a file. This
is the extension's single capability - the conversion runs entirely in-process (Apache POI + PDFBox),
with no external API call involved.

## Request Details
- **Area**: Document Conversion
- **Type**: CHANGE_SYSTEM
- **External API**: None - conversion is performed in-process using Apache POI (to read the Word
  document) and PDFBox (to render the PDF).

## Input Parameters
| Parameter | Type | Required | Description |
|---|---|---|---|
| File | File | Yes | The Word document to convert. Only `.doc` and `.docx` files are supported. |

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| PDF File | File | The generated PDF. Present only when the conversion succeeds. |

## Example
**Input**: File=`quarterly-report.docx`
**Output**: PDF File=`quarterly-report.pdf` (binary file attachment)

## Error Handling
- **Missing file** - "File is required. Please attach a Word document (.doc or .docx)."
- **Unsupported file type** - any extension other than `.doc`/`.docx` is rejected with an actionable
  message before conversion is attempted.
- **Empty or corrupted file** - the file's content is checked against the expected format's magic bytes
  (ZIP/OOXML for `.docx`, OLE2 for `.doc`); a mismatch is reported as a validation error rather than
  failing deep inside the converter.
- **Password-protected document** - reported as "This document is password-protected and can't be
  converted. Remove the password and try again."
- **Unparseable document** - a file that passes the extension/magic-byte check but whose internal
  structure is still malformed is reported as "The document couldn't be parsed - it may be corrupted or
  use an unsupported Word format variant."

## Technical Implementation
`.docx` files are parsed with Apache POI's XWPF model (paragraphs, runs, tables) and rendered to PDF
with PDFBox, preserving paragraph text, bold/italic emphasis, left/center/right alignment, and tables
(rendered as plain delimited rows). Legacy `.doc` files are read with Apache POI's HWPF
`WordExtractor` and rendered as plain paragraphs, without per-run styling - the legacy binary format's
run-level style API is considerably more involved than XWPF's, and style fidelity for the increasingly
rare legacy format was judged not worth that added complexity. Both paths share the same line-wrapping
and page-break rendering engine.
