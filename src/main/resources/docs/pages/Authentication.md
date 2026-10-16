# Authentication

EB Doc To Pdf Converter requires **no authentication**. It does not call any external API - the
conversion from Word to PDF happens entirely in-process inside the extension, using the open-source
Apache POI and PDFBox libraries. There is no account to create, no token or API key to obtain, and no
Setup-tab credential field to fill in.

## Obtaining credentials

Not applicable - there are no credentials to obtain for this extension.

## Required scopes / permissions

Not applicable - no external system is called, so no scopes or permissions are required.

## Verifying

Run **Test Connection** in the Setup area. It doesn't check connectivity to an external system (there
isn't one); instead it runs a real, tiny document conversion through the same code path used by the
Convert Document to PDF request, proving the bundled libraries initialize and work correctly in your
environment.
