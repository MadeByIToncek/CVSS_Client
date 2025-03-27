package space.itoncek.cvss.client.api;

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

    public long getPing() throws IOException {
        Request request = new Request.Builder().url(url + "/time").build();

        long start = System.currentTimeMillis();
        Response execute = client.newCall(request).execute();
        long end = System.currentTimeMillis();

        assert execute.body() != null;
        long mid = Long.parseLong(execute.body().string());
        execute.close();
        return ((mid - start) + (end - mid)) / 2L;
    }

    public @Nullable ArrayList<Team> listTeams() throws IOException, JSONException {
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
}
