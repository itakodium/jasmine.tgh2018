package com.example.takumi.uimock;

public class Callee {
    private String name;
    private int icon;
    private String number;

    public Callee(String name, int icon, String addr) {
        this.name = name;
        this.icon = icon;
        this.number = addr;
    }
    String getName() { return this.name; }
    String getNumber() { return this.number; }
    int getIcon() { return this.icon; }
}
