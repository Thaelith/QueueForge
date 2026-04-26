package com.queueforge.domain;

public enum JobPriority {
    LOW(0),
    NORMAL(50),
    HIGH(100),
    CRITICAL(200);

    private final int value;

    JobPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static JobPriority fromValue(int value) {
        for (JobPriority p : values()) {
            if (p.value == value) {
                return p;
            }
        }
        return NORMAL;
    }
}
