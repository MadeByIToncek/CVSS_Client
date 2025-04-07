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
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

import kotlin.Pair;
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
            //noinspection ResultOfMethodCallIgnored
            f.mkdirs();
            //noinspection ResultOfMethodCallIgnored
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
        } catch (Exception e) {
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

                JSONArray members = o.getJSONArray("members");
                List<String> mem = new ArrayList<>();
                for (int j = 0; j < members.length(); j++) {
                    mem.add(members.getString(j));
                }
                at.add(new Team(o.getInt("id"), o.getString("name"), o.getString("colorDark"), o.getString("colorBright"), mem));
            }
            return at;
        } catch (IOException e) {
            return null;
        }
    }

    public void updateTeam(int teamId, @NotNull String teamName, @NotNull String colorBright, @NotNull String colorDark) throws JSONException, IOException {
        Request request = new Request.Builder().patch(RequestBody.create(new JSONObject()
                .put("id", teamId)
                .put("name", teamName)
                .put("colorBright", colorBright)
                .put("colorDark", colorDark)
                .toString(4), MediaType.parse("application/json"))).url(url + "/teams/team").build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            if (!res.body().string().trim().equals("ok")) {
                throw new IOException("Unable to update the team!");
            }
        }
    }

    public void updateTeamMembers(int id, List<String> teamMembers) throws JSONException, IOException {
        JSONArray a = new JSONArray();
        for (String s : teamMembers) {
            a.put(s);
        }

        Request request = new Request.Builder()
                .patch(RequestBody.create(new JSONObject()
                        .put("id", id)
                        .put("members", a)
                        .toString(4), MediaType.parse("application/json")))
                .url(url + "/teams/teamMembers").build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
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
            JSONArray members = o.getJSONArray("members");
            List<String> mem = new ArrayList<>();
            for (int i = 0; i < members.length(); i++) {
                mem.add(members.getString(i));
            }
            return new Team(o.getInt("id"), o.getString("name"), o.getString("colorDark"), o.getString("colorBright"), mem);
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
            assert res.body() != null;
            if (!res.body().string().trim().equals("ok")) {
                throw new IOException("Unable to update the team!");
            }
        }
    }

    public EventStreamWebsocketHandler createEventHandler(@NotNull Consumer<EventStreamWebsocketHandler.Event> handleEvent,
                                                          @NotNull Consumer<String> wsFail) {
        return new EventStreamWebsocketHandler(url + "/stream/event") {

            @Override
            public void handleEvent(Event e) {
                handleEvent.accept(e);
            }

            @Override
            public void wsFail(String reason) {
                wsFail.accept(reason);
            }
        };
    }

    public TimeStreamWebsocketHandler createTimeHandler(@NotNull Consumer<Integer> timeTick, @NotNull Consumer<String> wsFail) {
        return new TimeStreamWebsocketHandler(url + "/stream/time") {
            @Override
            public void timeTick(int i) {
                timeTick.accept(i);
            }

            @Override
            public void gameStart() {

            }

            @Override
            public void gameEnd() {

            }

            @Override
            public void wsFail(String reason) {
                wsFail.accept(reason);
            }
        };
    }

    public boolean deleteTeam(int teamId) throws JSONException, IOException {
        Request request = new Request.Builder()
                .url(url + "/teams/team")
                .delete(RequestBody.create(new JSONObject().put("id", teamId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            return res.body().string().trim().equals("ok");
        }
    }

    public boolean deleteMatch(int matchId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(url + "/teams/match")
                .delete(RequestBody.create(new JSONObject().put("id", matchId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            return res.body().string().trim().equals("ok");
        }
    }

    public boolean createTeam(@NotNull String teamName,@NotNull String colorBright,@NotNull String colorDark) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(url + "/teams/team")
                .post(RequestBody.create(new JSONObject()
                        .put("name", teamName)
                        .put("colorBright", colorBright)
                        .put("colorDark", colorDark)
                        .toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            return res.body().string().trim().equals("ok");
        }
    }

    public boolean createMatch(int leftTeamId, int rightTeamId) throws JSONException, IOException {
        Request request = new Request.Builder()
                .url(url + "/teams/match")
                .post(RequestBody.create(new JSONObject().put("leftTeamId", leftTeamId).put("rightTeamId", rightTeamId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            return res.body().string().trim().equals("ok");
        }
    }

    public boolean armMatch(int matchId) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(url + "/match/arm")
                .post(RequestBody.create(new JSONObject().put("id", matchId).toString(4), MediaType.parse("application/json")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            String ok = res.body().string();
            Log.i("debdeb", ok);
            return ok.trim().equals("ok");
        }
    }

    public boolean isInGame() throws IOException {
        Request request = new Request.Builder()
                .url(url + "/match/matchInProgress")
                .get()
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            String ok = res.body().string();
            Log.i("debdeb", ok);
            return Boolean.parseBoolean(ok);
        }
    }

    public boolean isArmed() throws IOException {
        Request request = new Request.Builder()
                .url(url + "/match/matchArmed")
                .get()
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            String ok = res.body().string();
            Log.i("debdeb", ok);
            return Boolean.parseBoolean(ok);
        }
    }

    public boolean startMatch() throws IOException {
        Request request = new Request.Builder()
                .url(url + "/match/start")
                .post(RequestBody.create("", MediaType.get("text/plain")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            String ok = res.body().string();
            Log.i("debdeb", ok);
            return ok.trim().equals("ok");
        }
    }

    public boolean toggleOverlay(OverlayPart part, boolean state) throws IOException {
        String target = "/overlay/";
        target += switch (part) {
            case Left -> "left/";
            case Right -> "right/";
            case Timer -> "timer/";
        };
        target += state ? "show" : "hide";

        Request request = new Request.Builder()
                .url(url + target)
                .put(RequestBody.create("", MediaType.get("text/plain")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            String ok = res.body().string();
            Log.i("debdeb", ok);
            return ok.trim().equals("ok");
        }
    }

    public boolean resetMatch() throws IOException {
        Request request = new Request.Builder()
                .url(url + "/match/reset")
                .post(RequestBody.create("", MediaType.get("text/plain")))
                .build();

        try (Response res = client.newCall(request).execute()) {
            assert res.body() != null;
            String ok = res.body().string();
            Log.i("debdeb", ok);
            return ok.trim().equals("ok");
        }
    }

    public int getMatchLength() throws IOException {
        Request request = new Request.Builder().url(url + "/defaultMatchLength").get().build();

        try (Response res = client.newCall(request).execute()) {
            if(res.code() == 200) {
                assert res.body() != null;
                return Integer.parseInt(res.body().string());
            } else return -1;
        }
    }

    @NotNull
    public Pair<Integer, Integer> getCurrentMatchScore() throws IOException, JSONException {
        Request request = new Request.Builder().url(url + "/score/matchScore").build();
        try (Response execute = client.newCall(request).execute()) {
            assert execute.body() != null;
            String s = execute.body().string();
            Log.i("debdeb", s);
            JSONObject body = new JSONObject(s);
            return new Pair<>(body.getInt("left"),body.getInt("right"));
        }
    }
    @NotNull
    public Pair<Team, Team> getCurrentMatchSides() throws IOException, JSONException {
        Request left = new Request.Builder().url(url + "/match/leftTeamId").build();
        Request right = new Request.Builder().url(url + "/match/rightTeamId").build();
        try (Response lexec = client.newCall(left).execute();
             Response rexec = client.newCall(right).execute()) {
            assert lexec.body() != null;
            assert rexec.body() != null;
            String ls = lexec.body().string();
            String rs = rexec.body().string();
            Log.i("debdeb", ls);
            Log.i("debdeb", rs);
            return new Pair<>(getTeam(Integer.parseInt(ls)),getTeam(Integer.parseInt(rs)));
        }
    }

    public enum OverlayPart {
        Left,
        Right,
        Timer
    }
}
