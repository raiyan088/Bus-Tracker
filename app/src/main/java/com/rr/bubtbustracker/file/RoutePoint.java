package com.rr.bubtbustracker.file;

public class RoutePoint {
    private final String id;
    private final String name;

    public RoutePoint(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
