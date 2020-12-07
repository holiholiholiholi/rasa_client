package de.dfki.et.tech4comp.converstation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    String id;
    List<Message> messages;

    @JsonIgnore
    public int size() {
        return messages == null ? 0 : messages.size();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        String date;
        String userId;
        String text = "";

        @Override
        public String toString() {
            return date + ":" + userId + ":" + text;
        }
    }
}

