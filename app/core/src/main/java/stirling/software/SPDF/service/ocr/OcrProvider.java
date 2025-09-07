package stirling.software.SPDF.service.ocr;

import java.io.IOException;

import stirling.software.SPDF.model.api.misc.ProcessPdfWithVisionOcrRequest;

public interface OcrProvider {
    String getProviderName();

    byte[] performOcr(ProcessPdfWithVisionOcrRequest request)
            throws IOException, InterruptedException;
}
