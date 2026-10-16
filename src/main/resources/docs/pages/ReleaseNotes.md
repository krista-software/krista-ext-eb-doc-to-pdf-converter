# Release Notes

- **Developer Name** : Krista Extension Builder
- **Krista Service APIs Java** : 1.0.126
- **Global Catalog Version** : Not Available

## Version 1.0.0 (October 2026)

Initial release.

### New Features
* Convert an uploaded Word document (`.doc` or `.docx`) to PDF entirely in-process (Apache POI +
  PDFBox) - no external API or credentials required.

### Resolved Bugs
* Not Available

### Dependency Updates
* Not Available

### Known Issues
* Legacy `.doc` conversion renders plain paragraph text only (no bold/italic/alignment) - see
  [Convert Document to PDF](pages/ConvertDocumentToPdf.md#technical-implementation).

### Limitations and Caveats
* Only `.doc` and `.docx` source documents are supported (per the approved scope); other formats
  (`.xlsx`, `.pptx`, `.txt`, `.html`, images, etc.) are rejected.
* Table rendering is simplified (plain delimited rows), not a pixel-accurate re-layout of Word's own
  table rendering.
* No authentication, Setup-tab configuration, or external system is involved.
