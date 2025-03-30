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

public abstract class EventStreamWebsocketHandler extends WebSocketListener implements Closeable {
    OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    public EventStreamWebsocketHandler(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newWebSocket(request, this);
    }

    public abstract void teamUpdateEvent();

    public abstract void matchUpdateEvent();
    public abstract void matchArmEvent();
    public abstract void matchStartEvent();
    public abstract void matchEndEvent();

    @Override
    public void onOpen(WebSocket webSocket, @NonNull Response response) {
        webSocket.send("Hello...");
        webSocket.send("...World!");
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        switch (Event.valueOf(text)) {
            case TEAM_UPDATE_EVENT -> teamUpdateEvent();
            case MATCH_UPDATE_EVENT -> matchUpdateEvent();
            case MATCH_ARM -> matchArmEvent();
            case MATCH_START -> matchStartEvent();
            case MATCH_END -> matchEndEvent();
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
    public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
        Log.e(this.getClass().getName(), "WS Fault", t);
    }

    @Override
    public void close() throws IOException {
        client.dispatcher().executorService().shutdown();
        client = null;
    }

    public enum Event {
        TEAM_UPDATE_EVENT,
        MATCH_UPDATE_EVENT,
        MATCH_ARM,
        MATCH_START,
        MATCH_END
    }
}