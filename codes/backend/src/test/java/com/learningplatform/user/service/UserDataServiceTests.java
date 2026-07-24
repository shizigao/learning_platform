package com.learningplatform.user.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Sql("/sql/user-schema.sql")
@Transactional
class UserDataServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Test
    void createsUserAndAssignsRoleIdempotently() {
        User user = newUser("alice", "alice@example.com");

        userService.create(user);

        assertThat(user.getId()).isNotNull();
        assertThat(userService.findByUsername("alice"))
                .get()
                .extracting(User::getNickname, User::getStatus)
                .containsExactly("Alice", UserStatus.ACTIVE);
        assertThat(userService.emailExists("alice@example.com", null)).isTrue();

        assertThat(roleService.assignRole(user.getId(), RoleCode.USER, null)).isTrue();
        assertThat(roleService.assignRole(user.getId(), RoleCode.USER, null)).isFalse();
        assertThat(roleService.findRoleCodesByUserId(user.getId()))
                .isEqualTo(Set.of(RoleCode.USER));
    }

    @Test
    void updatesProfileLoginAndStatus() {
        User user = userService.create(newUser("bob", null));
        user.setNickname("Bob Updated");
        user.setPhone("13800000000");
        user.setBio("Learning every day");

        userService.updateProfile(user);
        userService.recordLogin(user.getId(), "127.0.0.1");
        userService.updateStatus(user.getId(), UserStatus.LOCKED);

        User updated = userService.getRequiredById(user.getId());
        assertThat(updated.getNickname()).isEqualTo("Bob Updated");
        assertThat(updated.getPhone()).isEqualTo("13800000000");
        assertThat(updated.getLastLoginAt()).isNotNull();
        assertThat(updated.getLastLoginIp()).isEqualTo("127.0.0.1");
        assertThat(updated.getStatus()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    void rejectsOperationsForMissingUser() {
        assertThatThrownBy(() -> roleService.assignRole(999L, RoleCode.USER, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.NOT_FOUND));
    }

    private User newUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$test-only-password-hash");
        user.setNickname(Character.toUpperCase(username.charAt(0)) + username.substring(1));
        user.setEmail(email);
        return user;
    }
}
