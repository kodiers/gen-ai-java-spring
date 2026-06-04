package com.kodiers.genaijavaspring.chat.multimodality.texttospeech;

public record TicketResponse(
        String ticketId,
        TicketStatus status,
        String message
) {
}

