package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter;

import app.krista.extension.impl.anno.ChangeLog;
import app.krista.extension.impl.anno.Extension;
import app.krista.extension.impl.anno.InvokerRequest;
import app.krista.extension.impl.anno.Java;
import app.krista.extension.impl.anno.StaticResource;

import java.util.Map;

/**
 * EB Doc To Pdf Converter extension entry point.
 *
 * No connection/authentication is required: document conversion runs entirely in-process (Apache POI
 * + PDFBox), with no external API or credentials involved - so this class declares no connection
 * {@code @Field}s and no Setup/Authentication-tab lifecycle hooks, only the Documentation tab.
 */
@Extension(name = "EB Doc To Pdf Converter", version = "1.0.0",
        description = "Converts an uploaded Word document (.doc or .docx) to PDF. Runs entirely "
                + "in-process using Apache POI and PDFBox - no external API or credentials required.")
@StaticResource(path = "docs", file = "docs")
@Java(version = Java.Version.JAVA_21)
@ChangeLog(file = "docs/pages/ReleaseNotes.md")
public final class EbDocToPdfConverterExtension {

    /** Setup-tab tabs. Only Documentation — there is no Setup/Authentication tab for this extension. */
    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Documentation", "static/docs");
    }
}
