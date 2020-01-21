package com.company;

import java.util.ArrayList;

public class Group {

    private String groupname;
    private ArrayList<Integer> members;
    private ArrayList<Integer> admins;


    // constructor
    Group(String groupname, Integer creator) {
        this.groupname = groupname;
        members = new ArrayList<Integer>();
        admins = new ArrayList<Integer>();
        members.add(creator);
        admins.add(creator);
    }

    String getName() {
        return groupname;
    }

    Boolean isAdmin (Integer user){
        return this.admins.stream().anyMatch(c ->c==user);
    }

    Boolean isMember (Integer user){
        return this.members.stream().anyMatch(c ->c==user);
    }

    void addMember(Integer user){
        this.members.add(user);
    }

    void deleteMember(Integer user){
        this.members.remove(user);
    }

    ArrayList<Integer> getMembers(){
        return this.members;
    }
}
