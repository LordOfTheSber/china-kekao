package dev.kekao.study;

public enum Rating {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4);

    private final int value;

    Rating(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Rating fromValue(int value) {
        for (Rating r : values()) {
            if (r.value == value) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown rating value: " + value);
    }
}
