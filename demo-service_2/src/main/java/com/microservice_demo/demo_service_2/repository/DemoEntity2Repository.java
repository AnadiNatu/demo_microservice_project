package com.microservice_demo.demo_service_2.repository;

import com.microservice_demo.demo_service_2.entity.DemoEntity2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoEntity2Repository extends JpaRepository<DemoEntity2 , Long>{

}
