package org.zella.tuapse.model.messages;

public class Message {

    public String peerId;
    public String data;

    public Message() {
    }

    @Override
    public String toString() {
        return "Message{" +
                "peerId='" + peerId + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
