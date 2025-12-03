package com.microservice_demo.demo_service_1.repository;

import com.microservice_demo.demo_service_1.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {}