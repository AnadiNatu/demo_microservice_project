package com.microservice_demo.demo_service_1.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY , generator = "ds1_users_seq")
    @SequenceGenerator(name = "ds1_users_seq" , sequenceName = "ds1_users_seq" , allocationSize = 1 , initialValue = 10000)
    private Long userId;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_id")
    private String providerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> role = new HashSet<>();

    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;

    @Column(name = "de1_connection_flag" , nullable = false , columnDefinition = "boolean default false")
    private Boolean de1ConnectionFlag = false;

    @Column(name = "de2_connection_flag" , nullable = false , columnDefinition = "boolean default false")
    private Boolean de2ConnectionFlag = false;

    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    @PrePersist
    protected void onCreate() {
        createdOn = LocalDateTime.now();
        updatedOn = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedOn = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return this.email; // or this.name, but email is unique.
    }

    @Override
    public String getPassword() {
        return ""; // Demo-Service1 does not authenticate users, only Auth-Service does.
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
