# Configuration

There is nothing to configure. This extension declares no connection fields on its Setup tab - document
conversion runs entirely in-process (Apache POI + PDFBox), so there is no base URL, API key, or other
setting to provide.

## Setup tab fields

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| *(none)* | - | - | This extension has no configurable connection fields. |

## Required vs optional

Not applicable - there is nothing to save or validate on the Setup tab.

## Notes

- Installing the extension is enough to start using it; there is no setup step before calling
  **Convert Document to PDF**.
- Run **Test Connection** (Setup area) at any time to confirm the bundled conversion libraries
  initialize correctly in your Krista environment.
