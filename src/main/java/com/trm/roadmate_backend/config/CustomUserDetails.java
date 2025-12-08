package com.trm.roadmate_backend.config;

import com.trm.roadmate_backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

// User 엔티티의 ID를 직접 포함하기 위한 UserDetails 확장
public class CustomUserDetails implements UserDetails {

    private final Integer userId; // Primary Key
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getUserId(); // User 엔티티의 userId 필드를 사용한다고 가정
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // ✨ 핵심: Controller/Service에서 바로 사용할 userId Getter
    public Integer getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // 계정 만료, 잠금, 비밀번호 만료, 활성화 여부는 모두 true로 간주 (필요 시 수정)
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}