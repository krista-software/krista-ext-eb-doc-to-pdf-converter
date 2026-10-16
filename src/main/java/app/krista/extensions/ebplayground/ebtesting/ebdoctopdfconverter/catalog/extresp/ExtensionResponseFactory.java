package app.krista.extensions.ebplayground.ebtesting.ebdoctopdfconverter.catalog.extresp;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.executor.RemediationActions;

import java.util.List;
import java.util.Map;

/** Builds {@link ExtensionResponse}s consistently across the Area classes. */
public final class ExtensionResponseFactory {
    private ExtensionResponseFactory() {}

    public static ExtensionResponse ok(Map<String, Object> values) {
        return new ExtensionResponseBuilder().success(values).build();
    }

    public static ExtensionResponse failure(String message, ExtensionResponse.Error.ExceptionType type) {
        ExtensionResponse.Error error = new ExtensionResponse.Error(message, System.currentTimeMillis(), type, "");
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error,
                new RemediationActions(List.of(), null), Map.of());
    }
}
