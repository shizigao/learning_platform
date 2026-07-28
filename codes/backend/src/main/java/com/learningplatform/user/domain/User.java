/* 文件职责：表示用户领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.domain;

import com.learningplatform.common.model.BaseEntity;

import java.time.LocalDateTime;

/**
 * 表示用户领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class User extends BaseEntity {
    /** 保存username，供该类型的业务逻辑读取或更新。 */
    private String username;
    /** 保存passwordHash，供该类型的业务逻辑读取或更新。 */
    private String passwordHash;
    /** 保存nickname，供该类型的业务逻辑读取或更新。 */
    private String nickname;
    /** 保存头像Url，供该类型的业务逻辑读取或更新。 */
    private String avatarUrl;
    /** 保存email，供该类型的业务逻辑读取或更新。 */
    private String email;
    /** 保存phone，供该类型的业务逻辑读取或更新。 */
    private String phone;
    /** 保存gender，供该类型的业务逻辑读取或更新。 */
    private String gender;
    /** 保存bio，供该类型的业务逻辑读取或更新。 */
    private String bio;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private UserStatus status;
    /** 保存lastLogin时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime lastLoginAt;
    /** 保存lastLoginIp，供该类型的业务逻辑读取或更新。 */
    private String lastLoginIp;

    /** 返回Username。 */
    public String getUsername() {
        return username;
    }

    /** 更新Username；调用方仍需遵守所属领域的校验规则。 */
    public void setUsername(String username) {
        this.username = username;
    }

    /** 返回PasswordHash。 */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** 更新PasswordHash；调用方仍需遵守所属领域的校验规则。 */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** 返回Nickname。 */
    public String getNickname() {
        return nickname;
    }

    /** 更新Nickname；调用方仍需遵守所属领域的校验规则。 */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 返回头像Url。 */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /** 更新头像Url；调用方仍需遵守所属领域的校验规则。 */
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /** 返回Email。 */
    public String getEmail() {
        return email;
    }

    /** 更新Email；调用方仍需遵守所属领域的校验规则。 */
    public void setEmail(String email) {
        this.email = email;
    }

    /** 返回Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 更新Phone；调用方仍需遵守所属领域的校验规则。 */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** 返回Gender。 */
    public String getGender() {
        return gender;
    }

    /** 更新Gender；调用方仍需遵守所属领域的校验规则。 */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /** 返回Bio。 */
    public String getBio() {
        return bio;
    }

    /** 更新Bio；调用方仍需遵守所属领域的校验规则。 */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /** 返回状态。 */
    public UserStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    /** 返回LastLogin时间。 */
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    /** 更新LastLogin时间；调用方仍需遵守所属领域的校验规则。 */
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    /** 返回LastLoginIp。 */
    public String getLastLoginIp() {
        return lastLoginIp;
    }

    /** 更新LastLoginIp；调用方仍需遵守所属领域的校验规则。 */
    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }
}
