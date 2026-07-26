package com.learningplatform.classroom.domain;

import java.time.LocalDateTime;

public class ClassMemberView extends ClassMember {
    private String username;
    private String nickname;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
