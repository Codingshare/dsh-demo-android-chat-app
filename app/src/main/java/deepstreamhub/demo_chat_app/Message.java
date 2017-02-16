package deepstreamhub.demo_chat_app;

/**
 * Created by alexharley on 16/02/17.
 */

public class Message {

    private String writer;
    private String content;

    public Message(String writer, String content) {
        this.writer = writer;
        this.content = content;
    }

    public String getWriter() {
        return writer;
    }

    public void setWriter(String writer) {
        this.writer = writer;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
