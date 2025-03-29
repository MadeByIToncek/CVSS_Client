package space.itoncek.cvss.client.api;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import space.itoncek.cvss.client.api.objects.Match;
import space.itoncek.cvss.client.api.objects.Team;

public class CVSSAPI {
    String url = "";
    final OkHttpClient client = new OkHttpClient();

    public CVSSAPI(File f) throws IOException {
        if (f == null) {
            url = "http://localhost:4444";
            return;
        }
        File cfgfile = new File(f + "/config.cfg");
        if (!cfgfile.exists()) {
            f.mkdirs();
            cfgfile.createNewFile();
        }

        try (Scanner sc = new Scanner(cfgfile)) {
            while (sc.hasNextLine()) {
                url += sc.nextLine();
            }
        }
    }

    public long getPing() {
        try {
            Request request = new Request.Builder().url(url + "/time").build();

            long start = System.currentTimeMillis();
            Response execute = client.newCall(request).execute();
            long end = System.currentTimeMillis();

            assert execute.body() != null;
            long mid = Long.parseLong(execute.body().string());
            execute.close();
            return ((mid - start) + (end - mid)) / 2L;
        } catch (IOException e) {
            return -1;
        }
    }

    @NotNull
    public String getVersion() {
        Request request = new Request.Builder().url(url).build();
        try (Response execute = client.newCall(request).execute()) {
            if (execute.body() != null) {
                return execute.body().string();
            } else {
                return "v-1.-1.-1.-1";
            }
        } catch (IOException e) {
            return e.toString();
        }
    }

    public @Nullable ArrayList<Team> listTeams() throws JSONException {
        Request request = new Request.Builder().url(url + "/teams/teams").build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            JSONArray arr = new JSONArray(res.body().string());
            ArrayList<Team> at = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                at.add(new Team(o.getInt("id"), o.getString("name")));
            }
            return at;
        } catch (IOException e) {
            return null;
        }
    }

    public void updateTeam(int teamId, @NotNull String teamName) throws JSONException, IOException {
        Request request = new Request.Builder().patch(RequestBody.create(new JSONObject().put("id", teamId).put("name", teamName).toString(4), MediaType.parse("application/json"))).url(url + "/teams/team").build();

        try (Response res = client.newCall(request).execute()) {
            if (!res.body().string().trim().equals("ok")) {
                throw new IOException("Unable to update the team!");
            }
        }
    }

    public @Nullable ArrayList<Match> listMatches() throws IOException, JSONException {
        Request request = new Request.Builder().url(url + "/teams/matches").build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            String body = res.body().string();
            JSONArray arr = new JSONArray(body);
            ArrayList<Match> at = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                at.add(new Match(o.getInt("id"), Match.State.valueOf(o.getString("state")), Match.Result.valueOf(o.getString("result")), getTeam(o.getInt("leftTeamId")), getTeam(o.getInt("rightTeamId"))));
            }
            return at;
        }
    }

    public @Nullable Team getTeam(int teamId) throws JSONException, IOException {
        Request request = new Request.Builder()
                .url(url + "/teams/team")
                .put(RequestBody.create(new JSONObject().put("id", teamId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            if (res.body() == null) return null;
            JSONObject o = new JSONObject(res.body().string());
            return new Team(o.getInt("id"), o.getString("name"));
        }
    }

    public @Nullable Match getMatch(int matchId) throws JSONException, IOException {
        Request request = new Request.Builder()
                .url(url + "/teams/match")
                .put(RequestBody.create(new JSONObject().put("id", matchId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            if (res.body() == null) return null;
            JSONObject o = new JSONObject(res.body().string());
            return new Match(o.getInt("id"), Match.State.valueOf(o.getString("state")), Match.Result.valueOf(o.getString("result")), getTeam(o.getInt("leftTeamId")), getTeam(o.getInt("rightTeamId")));
        }
    }

    public void updateMatch(int matchId, @NotNull Match.State matchState, @NotNull Match.Result result, int leftTeamId, int rightTeamId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .patch(RequestBody.create(new JSONObject()
                                .put("id", matchId)
                                .put("matchState", matchState)
                                .put("result", result)
                                .put("leftTeamId", leftTeamId)
                                .put("rightTeamId", rightTeamId)
                                .toString(4),
                        MediaType.parse("application/json")))
                .url(url + "/teams/match")
                .build();

        try (Response res = client.newCall(request).execute()) {
            if (!res.body().string().trim().equals("ok")) {
                throw new IOException("Unable to update the team!");
            }
        }
    }

    public EventStreamWebsocketHandler createEventHandler(@NotNull Runnable teamUpdateEvent, @NotNull Runnable matchUpdateEvent) {
        return new EventStreamWebsocketHandler(url + "/stream/event") {
            @Override
            public void teamUpdateEvent() {
                teamUpdateEvent.run();
            }

            @Override
            public void matchUpdateEvent() {
                matchUpdateEvent.run();
            }
        };
    }

    public boolean deleteTeam(int teamId) throws JSONException, IOException {
        Request request = new Request.Builder()
                .url(url + "/teams/team")
                .delete(RequestBody.create(new JSONObject().put("id", teamId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            return res.body().string().trim().equals("ok");
        }
    }

    public boolean deleteMatch(int matchId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(url + "/teams/match")
                .delete(RequestBody.create(new JSONObject().put("id", matchId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            return res.body().string().trim().equals("ok");
        }
    }

    public boolean createTeam(@NotNull String teamName) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(url + "/teams/team")
                .post(RequestBody.create(new JSONObject().put("name", teamName).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            return res.body().string().trim().equals("ok");
        }
    }

    public boolean createMatch(int leftTeamId, int rightTeamId) throws JSONException, IOException {
        Request request = new Request.Builder()
                .url(url + "/teams/match")
                .post(RequestBody.create(new JSONObject().put("leftTeamId", leftTeamId).put("rightTeamId", rightTeamId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            return res.body().string().trim().equals("ok");
        }
    }
}
