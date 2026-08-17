package controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/v1/incident")
@RequiredArgsConstructor
public class IncidentStreamController {

    private final OpenAiChatModel openAiChatModel;
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamIncidentAnalysis(
            @RequestParam("rawMessage") String rawMessage,
            @RequestParam(value = "temp", defaultValue = "0.5") Double temperature,
            @RequestParam(value = "maxTokens", defaultValue = "1000") Integer maxTokens
    ) {
        log.info("Nhận request stream sự cố: messageLength={}, temp={}, maxTokens={}",
                rawMessage.length(), temperature, maxTokens);
        OpenAiChatOptions dynamicOptions = OpenAiChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        Prompt prompt = new Prompt(
                "Phân tích chi tiết tiến trình xử lý sự cố logistics sau: " + rawMessage,
                dynamicOptions
        );
        return openAiChatModel.stream(prompt)
                .map(chatResponse -> {
                    String content = chatResponse.getResult().getOutput().getText();
                    return ServerSentEvent.<String>builder()
                            .data(content != null ? content : "")
                            .build();
                })
                .doOnCancel(() -> log.warn("Client đã ngắt kết nối SSE stream."))
                .doOnComplete(() -> log.info("Hoàn thành streaming phản hồi tới client."))
                .doOnError(error -> log.error("Lỗi xảy ra trong quá trình streaming SSE: {}", error.getMessage()));
    }
}