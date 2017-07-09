package rocks.pho.eth.util;

import com.alibaba.fastjson.JSONObject;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.LinkedBlockingQueue;
import java.sql.Timestamp;

/**
 * Created by phoenix on 7/3/17.
 */
public class WSClient extends WebSocketClient {

    public LinkedBlockingQueue queue = null;
    private SubModel marketDepthSubModel = new SubModel();
    private SubModel tradeDetailSubModel = new SubModel();

    public WSClient(String address, String marketDepthTopic, Long marketDepthTopicId, String tradeDetailTopic, Long tradeDetailId) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        super(new URI(address));

        this.queue = new LinkedBlockingQueue<String>();

        this.setSocket(this.getFactory().createSocket());
        this.connectBlocking();

        this.marketDepthSubModel.setSub(marketDepthTopic);
        this.marketDepthSubModel.setId(marketDepthTopicId);
        this.send(JSONObject.toJSONString(this.marketDepthSubModel));

        this.tradeDetailSubModel.setSub(tradeDetailTopic);
        this.tradeDetailSubModel.setId(tradeDetailId);
        this.send(JSONObject.toJSONString(this.tradeDetailSubModel));
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("opened connection");
        // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
    }

    @Override
    public void onMessage(String message) {
        System.out.println("received: " + message);
    }

    @Override
    public void onMessage(ByteBuffer socketBuffer) {
        try {
            String marketStr = CommonUtils.byteBufferToString(socketBuffer);
            String market = CommonUtils.uncompress(marketStr);
            if (market.contains("ping")) {
                // Client 心跳
                this.send(market.replace("ping", "pong"));
                System.out.println("sent heartbeat at " + new Timestamp((new Long(market.substring(8, 21)))).toString());
            } else {
                String marketDepthTopicStr = "\"ch\":\"" + this.marketDepthSubModel.getSub() + "\"";
                String tradeDetailTopicStr = "\"ch\":\"" + this.tradeDetailSubModel.getSub() + "\"";
                if (market.contains(marketDepthTopicStr) || market.contains(tradeDetailTopicStr)) {
                    if (!queue.offer(market)) {
                        System.out.println("queue offer error: " + market);
                    }
                } else {
                    System.out.println("error data!!!!!!!!!!!");
                    System.out.println(market);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFragment(Framedata fragment) {
        System.out.println("received fragment: " + new String(fragment.getPayloadData().array()));
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The codecodes are documented in class org.java_websocket.framing.CloseFrame
        System.out.println("Connection closed by " + (remote ? "remote peer" : "us"));
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }

    private SSLSocketFactory getFactory() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
        }};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        SSLSocketFactory factory = sslContext.getSocketFactory();
        return factory;
    }
}
