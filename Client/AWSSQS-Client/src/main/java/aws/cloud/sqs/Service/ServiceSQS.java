package aws.cloud.sqs.Service;

import aws.cloud.sqs.Entity.Result;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceSQS {

    public static List<Result> results = new ArrayList<Result>();
    public static List<String> keysNewFiles = new ArrayList<String>();

    @Autowired
    QueueMessagingTemplate messagingTemplate;

    public void send(String topicName, Object message) {
        messagingTemplate.convertAndSend(topicName, message);
    }

    @SqsListener("responseQueue")
    public void receiveMessage(String message, @Header("SenderId") String senderId) throws IOException {
        System.out.println(message);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        Result result = objectMapper.readValue(message, Result.class);
        results.add(result);
    }

    @SqsListener("outbox")
    public void receiveOutbox(String mess) {
        System.out.println(mess);
        keysNewFiles.add(mess);
    }

}
