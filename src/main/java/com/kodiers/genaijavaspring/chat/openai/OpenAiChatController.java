package com.kodiers.genaijavaspring.chat.openai;

import com.kodiers.genaijavaspring.chat.openai.dto.response.SummarizationResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/openai/chat")
public class OpenAiChatController {

    private static final String SYSTEM_PROMPT = "You are a helpful assistant that summarizes any given content. "
            + "Ensure that the summary is concise, informative, and captures the key points. "
            + "Use a friendly and approachable tone while maintaining a professionalism.";
//            + "Do not answer anything other than the summarization. If the question is not about summarization, "
//            + "respond with 'I am sorry, I cannot help with that.'";

    private final ChatClient chatClient;
    private final OpenAiService openAiService;

    @Value("classpath:/templates/summarize-prompt.st")
    private Resource summarizePrompt;

    public OpenAiChatController(@Qualifier("openAiChatClient") ChatClient chatClient, OpenAiService openAiService) {
        this.chatClient = chatClient;
        this.openAiService = openAiService;
    }

    @PostMapping("/summarize")
    public String summarize(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }

    @PostMapping(value = "/summarize-with-streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> summarizeWithStreaming(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .stream()
                .content()
                .bufferTimeout(40, Duration.ofMillis(200))
                .map(tokens -> String.join(" ", tokens));
    }

    @PostMapping("/summarize-meeting-notes")
    public String summarizeMeetingNotes(@RequestBody String meetingNotes) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("Can you summarize the following meeting notes: {meetingNotes}"
                + "Use the format as described in the following example while doing the summarization:"
                + "Input: In today’s sales strategy meeting, we reviewed Q3 targets and performance gaps. The team agreed to focus on enterprise clients "
                + "and strengthen partnerships. A proposal was made to expand into two new regions. "
                + "Marketing suggested aligning campaigns with sales objectives to improve lead conversion and shorten sales cycles"
                + "Output:"
                + "Action items:"
                + "* Focus on enterprise clients and partnerships.\n"
                + "* Explore expansion into two new regions.\n"
                + "* Align marketing campaigns with sales objectives.\n"
                + "Decisions:\n"
                + "* Enterprise clients prioritized for Q3.\n"
                + "* Marketing and sales to work jointly on lead conversion.")
                        .param("meetingNotes", meetingNotes))
                .call()
                .content();
    }

    @PostMapping("/summarize-meeting-notes-structured")
    public SummarizationResponse summarizeMeetingNotesStructuredOutput(@RequestBody String meetingNotes) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("Can you summarize the following meeting notes: {meetingNotes}"
                                + "Use the format as described in the following example while doing the summarization:"
                                + "Input: In today’s sales strategy meeting, we reviewed Q3 targets and performance gaps. The team agreed to focus on enterprise clients "
                                + "and strengthen partnerships. A proposal was made to expand into two new regions. "
                                + "Marketing suggested aligning campaigns with sales objectives to improve lead conversion and shorten sales cycles"
                                + "Output:"
                                + "Action items:"
                                + "* Focus on enterprise clients and partnerships.\n"
                                + "* Explore expansion into two new regions.\n"
                                + "* Align marketing campaigns with sales objectives.\n"
                                + "Decisions:\n"
                                + "* Enterprise clients prioritized for Q3.\n"
                                + "* Marketing and sales to work jointly on lead conversion.")
                        .param("meetingNotes", meetingNotes))
                .call()
                .entity(SummarizationResponse.class);
    }

    @PostMapping("/summarize-meeting-notes-structured-with-prompt-template")
    public SummarizationResponse summarizeMeetingNotesStructuredOutputAndPromptTemplate(@RequestBody String meetingNotes) {
        PromptTemplate promptTemplate = new PromptTemplate(summarizePrompt);
        Prompt prompt = promptTemplate.create(Map.of("meetingNotes", meetingNotes));
        return chatClient.prompt(prompt)
                .system(SYSTEM_PROMPT)
                .call()
                .entity(SummarizationResponse.class);
    }

    @PostMapping("/summarize-meeting-notes-structured-list")
    public List<SummarizationResponse> summarizeMeetingNotesStructuredOutputList(@RequestBody String meetingNotes) {
//        ChatOptions chatOptions = OpenAiChatOptions.builder()
//                .N(3)
//                .build();
        return chatClient.prompt()
//                .options(chatOptions)
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("Can you summarize the following meeting notes: {meetingNotes}"
                                + "Give me 3 different summarizations in the same format so that I can choose from."
                                + "Use the format as described in the following example while doing the summarization:"
                                + "Input: In today’s sales strategy meeting, we reviewed Q3 targets and performance gaps. The team agreed to focus on enterprise clients "
                                + "and strengthen partnerships. A proposal was made to expand into two new regions. "
                                + "Marketing suggested aligning campaigns with sales objectives to improve lead conversion and shorten sales cycles"
                                + "Output:"
                                + "Action items:"
                                + "* Focus on enterprise clients and partnerships.\n"
                                + "* Explore expansion into two new regions.\n"
                                + "* Align marketing campaigns with sales objectives.\n"
                                + "Decisions:\n"
                                + "* Enterprise clients prioritized for Q3.\n"
                                + "* Marketing and sales to work jointly on lead conversion.")
                        .param("meetingNotes", meetingNotes))
                .call()
                .entity(new ParameterizedTypeReference<>() {});
    }

    @PostMapping("/summarize-with-openai-java-client")
    public String summarizeMeetingNotesWithOpenAiJavaClient(@RequestBody String meetingNotes) throws OpenAiChatException {
        return openAiService.chat(meetingNotes);
    }
}
