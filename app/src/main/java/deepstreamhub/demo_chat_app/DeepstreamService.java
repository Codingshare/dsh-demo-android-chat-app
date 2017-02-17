package deepstreamhub.demo_chat_app;

import io.deepstream.DeepstreamClient;

import java.net.URISyntaxException;

/**
 * Created by alexharley on 16/02/17.
 */

public class DeepstreamService {

    private static DeepstreamService ourInstance = new DeepstreamService();
    DeepstreamClient deepstreamClient;




    public static DeepstreamService getInstance() {
        return ourInstance;
    }

    DeepstreamService() {
        try {
            this.deepstreamClient = new DeepstreamClient("10.0.2.2:6021");
        } catch (URISyntaxException e) {

            e.printStackTrace();
        }
    }

    DeepstreamClient getDeepstreamClient() {
        return this.deepstreamClient;
    }
}