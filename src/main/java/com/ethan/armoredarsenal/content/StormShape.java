package com.ethan.armoredarsenal.content;

public final class StormShape {
    public static final HeadSpec[] HEADS = {
            new HeadSpec(-5, 17, -5, 0), new HeadSpec(0, 18, -6, 0), new HeadSpec(5, 17, -5, 0),
            new HeadSpec(-21, 6, -8, 1), new HeadSpec(-16, 7, -9, 1), new HeadSpec(-11, 6, -8, 1),
            new HeadSpec(11, 6, -8, 2), new HeadSpec(16, 7, -9, 2), new HeadSpec(21, 6, -8, 2)
    };

    public record HeadSpec(double x, double y, double z, int group) {}

    private StormShape() {}
}
