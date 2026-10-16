# EB Doc To Pdf Converter — Implementation Plan

## Summary
A single-capability Krista extension that converts an uploaded Word document (.doc/.docx) to PDF.
Unlike most extensions, it does **not** call a third-party HTTP API: per the author's answer to the
provider question, conversion is done **in-process** with an open-source Java library (Apache POI to
parse the Word document, plus a compatible rendering library to produce the PDF bytes). There is no
remote system, no credentials, and no network call involved in the conversion itself.

- **Extension name (slug):** `eb-doc-to-pdf-converter`
- **Title:** EB Doc To Pdf Converter
- **Version:** 1.0.0
- **Package:** `app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter`
- **Ecosystem / Domain:** EB Playground / EB Testing

## Areas & Requests

### Area: Document Conversion

| Request | Type | Inputs | Output | External endpoint | Reused or new |
|---|---|---|---|---|---|
| Convert Document to PDF | CHANGE_SYSTEM | `File` (required) — the uploaded .doc/.docx | `File` — the generated PDF | N/A — local library conversion (Apache POI + PDF renderer), no external HTTP call | **New** |

Notes:
- Input/output use the platform's `app.krista.model.base.File` type, bridged via `KristaMediaClient`/
  `FileRepository` (upload bytes in, write converted PDF bytes out) per the business-logic
  file-handling pattern — not a raw byte array.
- Output is a plain `@Field.File`, not an `@Entity` — a converted file isn't a domain object, matching
  the existing "download/export" precedent in the skills (no entity needed for a pure file-in/file-out
  operation).
- Scope is restricted to Word documents (.doc/.docx) only, per the author's confirmation — no other
  source formats (xlsx, pptx, html, images, etc.) are in scope for this build.
- Tests (pure-Java, no framework mocking needed): happy path for .docx, happy path for legacy .doc,
  rejection of a non-Word file type, rejection of a missing file input, and graceful handling of a
  corrupted/unreadable document.

## Authentication
**Pattern: NONE.** The author explicitly chose "none" for authentication, and that's consistent with
how this extension actually works — it performs the conversion locally with a bundled library, so
there's no vendor account, API key, or login to configure. Per the NONE-pattern rule: no secured
connection fields, and no Setup/Authentication tab is required for this extension.

## Capabilities
| Capability | Included? | Why |
|---|---|---|
| Entity search | No | No searchable domain objects — this is a stateless transform |
| Webhooks | No | No event source |
| MCP (`tool=true`) | No | Not requested; can be added later if the conversion request should be agent-callable |
| Solutions tab | No | Not requested |
| Files | **Yes** | Both the input (uploaded Word doc) and output (generated PDF) are binary files handled via `KristaMediaClient`/`FileRepository` |

## Domain reuse
`domain-catalog.json` reports `"found": false` for EB Playground/EB Testing — this ecosystem/domain is
brand new, so there was nothing published to check against. The one area and request above are built
entirely fresh; nothing was reused, modified, or avoided.

## Placeholder IDs — must replace before deploy
`EB Playground` / `EB Testing` have never been through the real global-catalog publish process, so the
`@Domain` block (`domainId`, `ecosystemId`) will be filled with **stable placeholder UUIDs** so the
code compiles. These **must** be swapped for the real registered Krista ecosystem/domain IDs before
this extension can bind in the catalog or deploy to an appliance:
- `domainId` (`catEntryDomain_…`) for "EB Testing"
- `ecosystemId` (`catEntryEcosystem_…`) for "EB Playground"

`localDomainRequest_…` (the one catalog request) is a local, random-with-prefix id and does **not**
need replacing.
