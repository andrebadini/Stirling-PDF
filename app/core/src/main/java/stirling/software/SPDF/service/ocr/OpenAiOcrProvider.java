package stirling.software.SPDF.service.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import stirling.software.SPDF.model.api.misc.ProcessPdfWithVisionOcrRequest;
import stirling.software.common.service.CustomPDFDocumentFactory;
import stirling.software.common.util.TempFile;
import stirling.software.common.util.TempFileManager;

@Service
@RequiredArgsConstructor
public class OpenAiOcrProvider implements OcrProvider {

    private final TempFileManager tempFileManager;
    private final CustomPDFDocumentFactory pdfDocumentFactory;
    private final ObjectMapper objectMapper;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public byte[] performOcr(ProcessPdfWithVisionOcrRequest request)
            throws IOException, InterruptedException {

        MultipartFile inputFile = request.getFileInput();
        Map<String, String> settings = request.getProviderSettings();
        String apiKey = settings.get("apiKey");
        String model = settings.getOrDefault("model", "gpt-4o-mini");
        String detail = settings.getOrDefault("detail", "high");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OpenAI API key is required.");
        }

        StringBuilder resultText = new StringBuilder();

        try (TempFile tempInputFile = new TempFile(tempFileManager, ".pdf")) {
            inputFile.transferTo(tempInputFile.getFile());

            try (PDDocument document = pdfDocumentFactory.load(tempInputFile.getPath().toFile())) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();

                for (int i = 0; i < pageCount; i++) {
                    // 1. Render page to image
                    BufferedImage image =
                            pdfRenderer.renderImageWithDPI(i, 300); // 300 DPI for good quality

                    // 2. Convert to Base64
                    String base64Image = encodeImageToBase64(image);

                    // 3. Call OpenAI API
                    String pageText =
                            callOpenAiApi(
                                    apiKey, model, detail, base64Image, request.getOutputFormat());

                    resultText.append(pageText).append("\n\n---\n\n");
                }
            }
        }

        return resultText.toString().getBytes();
    }

    private String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String callOpenAiApi(
            String apiKey, String model, String detail, String base64Image, String outputFormat)
            throws IOException, InterruptedException {

        // Create OpenAI Request Body
        OpenAiRequest openAiRequest = new OpenAiRequest();
        openAiRequest.setModel(model);

        List<OpenAiMessage> messages = new ArrayList<>();

        // System Message
        String systemPrompt =
                "You are an OCR assistant. Extract all visible text from the image. Respond ONLY with the extracted content.";
        if ("markdown".equals(outputFormat)) {
            systemPrompt =
                    "You are an OCR assistant. Extract all visible text from the image, preserving the formatting in Markdown as much as possible (headings, lists, tables). Respond ONLY with the extracted content.";
        }
        messages.add(new OpenAiMessage("system", systemPrompt));

        // User Message with Image
        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of("type", "text", "text", "Extract the text from this image."));
        userContent.add(
                Map.of(
                        "type",
                        "image_url",
                        "image_url",
                        Map.of("url", "data:image/png;base64," + base64Image, "detail", detail)));
        messages.add(new OpenAiMessage("user", userContent));

        openAiRequest.setMessages(messages);
        openAiRequest.setMax_tokens(4096);

        // HttpClient
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(OPENAI_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        objectMapper.writeValueAsString(openAiRequest)))
                        .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(
                    "OpenAI API request failed with status code "
                            + response.statusCode()
                            + " and body: "
                            + response.body());
        }

        // Parse response
        OpenAiResponse openAiResponse =
                objectMapper.readValue(response.body(), OpenAiResponse.class);
        return openAiResponse.getChoices().get(0).getMessage().getContent();
    }

    // Inner classes for OpenAI request/response serialization
    @Data
    private static class OpenAiRequest {
        private String model;
        private List<OpenAiMessage> messages;
        private int max_tokens;
    }

    @Data
    private static class OpenAiMessage {
        private String role;
        private Object content; // Can be String or List of content parts

        public OpenAiMessage(String role, Object content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    private static class OpenAiResponse {
        private List<Choice> choices;

        @Data
        public static class Choice {
            private ResponseMessage message;
        }

        @Data
        public static class ResponseMessage {
            private String content;
        }
    }
}
