# Test Connection

## Overview
Verifies the bundled document-conversion libraries (Apache POI + PDFBox) initialize and work correctly
in this Krista environment. This extension has no external system to connect to, so this request runs a
real, tiny end-to-end conversion through the same code path as **Convert Document to PDF** instead of
checking connectivity/credentials.

## Request Details
- **Area**: Setup
- **Type**: QUERY_SYSTEM
- **External API**: None.

## Input Parameters
*(none)*

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Success | Boolean | `true` if the self-test conversion succeeded. |
| Message | Text | A human-readable result, or the failure reason. |

## Example
**Output**: Success=`true`, Message=`"Document conversion libraries are initialized and working."`

## Error Handling
- A failure (e.g. a missing/mismatched library on the classpath) returns Success=`false` with the
  underlying error message, and is logged at ERROR with the invoker id.
