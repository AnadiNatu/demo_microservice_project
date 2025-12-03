package com.microservice_demo.demo_service_1.repository;

import com.microservice_demo.demo_service_1.entity.DemoEntity1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoEntity1Repository extends JpaRepository<DemoEntity1 , Long> {
    @Query("SELECT de FROM DemoEntity1 de JOIN de.user u WHERE u.id = :userId")
    DemoEntity1 findByUserUserId(@Param("userId") Long userId);
}
