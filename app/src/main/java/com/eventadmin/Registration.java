package com.eventadmin;

public class Registration {
    public String userId;
    public String name;
    public String college;
    public int amount;
    public String coordinator;
    
    public Registration(String userId, String name, String college, int amount, String coordinator) {
        this.userId = userId;
        this.name = name;
        this.college = college;
        this.amount = amount;
        this.coordinator = coordinator;
    }
}
