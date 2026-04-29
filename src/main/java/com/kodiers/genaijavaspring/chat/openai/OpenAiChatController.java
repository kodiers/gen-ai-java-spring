package com.kodiers.genaijavaspring.chat.openai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/openai/chat")
public class OpenAiChatController {

    private static final String SYSTEM_PROMPT = "You are a helpful assistant that summarizes any given content. "
            + "Ensure that the summary is concise, informative, and captures the key points. "
            + "Use a friendly and approachable tone while maintaining a professionalism."
            + "Do not answer anything other than the summarization. If the question is not about summarization, "
            + "respond with 'I am sorry, I cannot help with that.'";

    private final ChatClient chatClient;
    private final OpenAiService openAiService;

    public OpenAiChatController(ChatClient chatClient, OpenAiService openAiService) {
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

    @PostMapping("/summarize-with-openai-java-client")
    public String summarizeMeetingNotesWithOpenAiJavaClient(@RequestBody String meetingNotes) throws OpenAiChatException {
        return openAiService.chat(meetingNotes);
    }
}
