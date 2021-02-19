package com.wgx.sgcc.config;

import com.wgx.sgcc.model.FileUpload;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FileRepository extends MongoRepository<FileUpload,String> {
}
