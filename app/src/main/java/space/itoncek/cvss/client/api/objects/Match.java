package space.itoncek.cvss.client.api.objects;

public record Match(int id, State state, Result result, Team left, Team right) {

    public enum State {
        UPCOMING,
        PLAYING,
        ENDED
    }

    public enum Result {
        LEFT_WON,
        RIGHT_WON,
        NOT_FINISHED
    }
}
