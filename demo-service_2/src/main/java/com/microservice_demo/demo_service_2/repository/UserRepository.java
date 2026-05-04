package com.microservice_demo.demo_service_2.repository;

import com.microservice_demo.demo_service_2.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users , Long>{

    Optional<Users> findByEmail(String email);

    boolean existsByEmail(String email);

}
