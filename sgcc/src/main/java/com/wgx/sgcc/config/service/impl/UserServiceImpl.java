package com.wgx.sgcc.config.service.impl;

import com.wgx.sgcc.config.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private MongoTemplate template;

    @Override
    public void insert(Map<String, Object> map, String collectionName) {
        template.insert(map, collectionName);
    }
}
