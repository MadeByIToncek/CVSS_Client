package space.itoncek.cvss.client.api;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public abstract class TimeStreamWebsocketHandler extends WebSocketListener implements Closeable {
    OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    public TimeStreamWebsocketHandler(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newWebSocket(request, this);
    }

    public abstract void timeTick(int i);
    public abstract void gameStart();
    public abstract void gameEnd();
    public abstract void wsFail(String reason);

    @Override
    public void onOpen(WebSocket webSocket, @NonNull Response response) {
        webSocket.send("Connected from android");
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        switch (Integer.parseInt(text)) {
            case -1 -> gameStart();
            case -2 -> gameEnd();
            default -> timeTick(Integer.parseInt(text));
        }
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, ByteString bytes) {
        System.out.println("MESSAGE: " + bytes.hex());
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, @NonNull String reason) {
        webSocket.close(1000, null);
        System.out.println("CLOSE: " + code + " " + reason);
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
        Log.e(this.getClass().getName(), "WS Fault", t);
    }

    @Override
    public void close() throws IOException {
        client.dispatcher().executorService().shutdown();
        client = null;
    }
}