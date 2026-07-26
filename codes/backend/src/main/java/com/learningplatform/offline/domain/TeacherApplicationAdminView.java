package com.learningplatform.offline.domain;

public class TeacherApplicationAdminView extends OfflineTeacherApplication {
    private String username;
    private String nickname;

    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getNickname() { return nickname; }
    public void setNickname(String value) { nickname = value; }
}
