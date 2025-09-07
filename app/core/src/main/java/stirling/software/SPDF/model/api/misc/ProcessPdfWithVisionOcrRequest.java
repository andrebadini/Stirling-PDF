package stirling.software.SPDF.model.api.misc;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import stirling.software.common.model.api.PDFFile;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessPdfWithVisionOcrRequest extends PDFFile {

    @Schema(
            description = "The OCR provider to use",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"openai", "gemini", "local", "paddleocr"})
    private String provider;

    @Schema(
            description = "The output format, e.g., 'text', 'markdown', 'json'",
            defaultValue = "markdown")
    private String outputFormat = "markdown";

    @Schema(
            description =
                    "A map of provider-specific settings. "
                            + "For 'openai': {apiKey, model, detail}. "
                            + "For 'gemini': {apiKey, model}. "
                            + "For 'local': {baseUrl, model}. ")
    private Map<String, String> providerSettings;
}
