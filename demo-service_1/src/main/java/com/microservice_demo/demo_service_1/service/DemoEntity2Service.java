package com.microservice_demo.demo_service_1.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoEntity2Service {

//    @Autowired
//    private DemoEntity2Repository repo;
//
//    @Autowired
//    private UserRepository usersRepo;
//
//    @Autowired
//    private DemoEntity1Repository demoEn1Repo;
//
//    public DemoEntity2 create(Long demoEn1Id, DemoEntity2 req) {
//
//        DemoEntity1 parent = demoEn1Repo.findById(demoEn1Id)
//                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity1 not found"));
//
//        req.setDemoEntity1(parent);
//
//        return repo.save(req);
//    }
//
//    public DemoEntity2 get(Long id) {
//        return repo.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("DemoEntity2 not found: " + id));
//    }
//
//    public DemoEntity2 createDE2(CreateDemoEntity2Dto dto){
//
//    }
//
//    public DemoEntity2Dto addUserListAndDE1(AddUserListAndDE1ToDE2Dto dto){
//
//    }
//
//    public DemoEntity2Dto addUserToDE1ListForDE2(AddUserToListDE1ForDE2Dto dto){
//
//    }
}
