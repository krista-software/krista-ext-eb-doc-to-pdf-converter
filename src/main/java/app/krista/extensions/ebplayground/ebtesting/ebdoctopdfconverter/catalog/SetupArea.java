package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.conversion.WordToPdfConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standard "Setup" area (Test Connection + Health Check). This extension has no external system to
 * connect to, so Test Connection instead runs a real end-to-end self-test of the bundled conversion
 * libraries (Apache POI + PDFBox) to prove they initialize and work correctly in this environment.
 */
@Domain(id = "catEntryDomain_738dcd68-fbd8-4207-a7b2-bac849a4362b",
        name = "EB Testing",
        ecosystemId = "catEntryEcosystem_e1557d10-6587-409e-aa1c-b1bbb4c42b17",
        ecosystemName = "EB Playground",
        ecosystemVersion = "7a6784f1-14fb-44cf-bf9d-60afe56e0cbb")
public class SetupArea {

    private static final Logger LOG = LoggerFactory.getLogger(SetupArea.class);

    private final String invokerId;

    @Inject
    public SetupArea(Invoker invoker) {
        this.invokerId = invoker.getInvokerId();
    }

    @CatalogRequest(
            id = "localDomainRequest_2a6e4c18-9d3b-4f7a-8e1c-5b2d7a9f3e60",
            name = "Test Connection",
            description = "Verifies the bundled document-conversion libraries initialize and work correctly.",
            area = "Setup",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Boolean(name = "Success", required = false)
    @Field.Text(name = "Message", required = false)
    public ExtensionResponse testConnection() {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            WordToPdfConverter.selfTest();
            out.put("Success", true);
            out.put("Message", "Document conversion libraries are initialized and working.");
        } catch (Exception e) {
            LOG.error("Test connection self-test failed for invoker {}", invokerId, e);
            out.put("Success", false);
            out.put("Message", "Document conversion self-test failed: " + e.getMessage());
        }
        return new ExtensionResponseBuilder().success(out).build();
    }

    @CatalogRequest(
            id = "localDomainRequest_5c9f1e3a-7b4d-4a2e-9c6f-1d8a3e5b7c92",
            name = "Health Check",
            description = "Checks extension and JVM health.",
            area = "Setup",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Healthy", required = false)
    @Field(name = "Heap Used MB", type = "Number", required = false)
    @Field.Text(name = "Details", required = false)
    public ExtensionResponse healthCheck() {
        Map<String, Object> out = new LinkedHashMap<>();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        long heapUsedMb = memBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        out.put("Is Healthy", true);
        out.put("Heap Used MB", (double) heapUsedMb);
        out.put("Details", "OK");
        return new ExtensionResponseBuilder().success(out).build();
    }
}
