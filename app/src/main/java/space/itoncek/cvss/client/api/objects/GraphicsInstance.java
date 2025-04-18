package space.itoncek.cvss.client.api.objects;

public record GraphicsInstance(String id, GraphicsMode mode) {

    public enum GraphicsMode {
        NONE,
        STREAM,
        TV_TWO_LEFT,
        TV_TWO_RIGHT,
        TV_THREE_LEFT,
        TV_THREE_RIGHT,
        TV_THREE_TIME
    }
}

