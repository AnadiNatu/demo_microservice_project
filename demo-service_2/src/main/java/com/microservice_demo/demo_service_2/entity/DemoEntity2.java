package com.microservice_demo.demo_service_2.entity;

import com.microservice_demo.demo_service_2.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DemoEntity2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long demoEn2Id;

    private String demoInfo;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    private int countField;
    private double priceField;

    // Store user IDs instead of Users entities
    @ElementCollection
    @CollectionTable(
            name = "demo_en2_user_ids",
            joinColumns = @JoinColumn(name = "demo_en2_id")
    )
    @Column(name = "user_id")
    private List<Long> userIds;

    // Store DemoEntity1 ID instead of the entity itself
    private Long demoEn1Id;
}


