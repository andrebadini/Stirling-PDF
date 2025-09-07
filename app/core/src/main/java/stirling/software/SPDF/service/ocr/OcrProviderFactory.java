package stirling.software.SPDF.service.ocr;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class OcrProviderFactory {

    private final Map<String, OcrProvider> providerMap;

    public OcrProviderFactory(List<OcrProvider> providers) {
        this.providerMap =
                providers.stream()
                        .collect(
                                Collectors.toMap(
                                        OcrProvider::getProviderName, Function.identity()));
    }

    public OcrProvider getProvider(String name) {
        OcrProvider provider = providerMap.get(name);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported OCR provider: " + name);
        }
        return provider;
    }
}
