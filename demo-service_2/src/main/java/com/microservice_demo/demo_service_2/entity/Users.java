package com.microservice_demo.demo_service_2.entity;


import com.microservice_demo.demo_service_2.enums.UserRoles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "ds2_users_seq" , sequenceName = "ds2_users_seq" , allocationSize = 1 , initialValue = 10000)
    private Long userId;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    private UserRoles role;

    @Column(nullable = false , columnDefinition = "boolean default false")
    private boolean de1ConnectionFlag;

    @Column(nullable = false , columnDefinition = "boolean default false")
    private boolean de2ConnectionFlag;

    @Column(name = "profile_picture")
    private String profilePicture;

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

    public void setDe1ConnectionFlag(boolean b) {
        this.de1ConnectionFlag = b;
    }
}