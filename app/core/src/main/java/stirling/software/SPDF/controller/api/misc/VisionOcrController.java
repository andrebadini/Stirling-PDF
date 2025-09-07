package stirling.software.SPDF.controller.api.misc;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.pixee.security.Filenames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import stirling.software.SPDF.model.api.misc.ProcessPdfWithVisionOcrRequest;
import stirling.software.SPDF.service.ocr.OcrProvider;
import stirling.software.SPDF.service.ocr.OcrProviderFactory;
import stirling.software.common.util.WebResponseUtils;

@RestController
@RequestMapping("/api/v1/misc")
@Tag(name = "Misc", description = "Miscellaneous APIs")
@Slf4j
@RequiredArgsConstructor
public class VisionOcrController {

    private final OcrProviderFactory ocrProviderFactory;

    @PostMapping(consumes = "multipart/form-data", value = "/vision-ocr")
    @Operation(
            summary = "Process a PDF file with an external Vision model for OCR",
            description =
                    "This endpoint uses an external vision model (like OpenAI, Gemini, etc.) to perform OCR on a PDF. Input:PDF Output:TXT/MD/JSON Type:S-I")
    public ResponseEntity<byte[]> processPdfWithVisionOcr(
            @ModelAttribute ProcessPdfWithVisionOcrRequest request)
            throws IOException, InterruptedException {

        // 1. Get provider from request and find the correct OcrProvider implementation
        OcrProvider provider = ocrProviderFactory.getProvider(request.getProvider());

        // 2. Call the provider's performOcr method
        byte[] result = provider.performOcr(request);

        // 3. Prepare the output filename
        MultipartFile inputFile = request.getFileInput();
        String outputFilename =
                Filenames.toSimpleFileName(inputFile.getOriginalFilename())
                                .replaceFirst("[.][^.]+$", "")
                        + "_vision_OCR."
                        + request.getOutputFormat();

        // 4. Return the result as a downloadable file
        return WebResponseUtils.bytesToWebResponse(result, outputFilename);
    }
}
