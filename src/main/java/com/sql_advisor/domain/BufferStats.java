package com.sql_advisor.domain;

public record BufferStats(
        long sharedHit,
        long sharedRead,
        long sharedDirtied,
        long sharedWritten,
        long tempRead,
        long tempWritten
) {
    public static final BufferStats EMPTY = new BufferStats(0, 0, 0, 0, 0, 0);

    public long totalShared() {
        return sharedHit + sharedRead;
    }

    public BufferStats plus(BufferStats other) {
        return new BufferStats(
                sharedHit + other.sharedHit,
                sharedRead + other.sharedRead,
                sharedDirtied + other.sharedDirtied,
                sharedWritten + other.sharedWritten,
                tempRead + other.tempRead,
                tempWritten + other.tempWritten);
    }
}
