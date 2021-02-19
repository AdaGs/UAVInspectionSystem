package com.wgx.sgcc.service.impl;

import com.wgx.sgcc.config.FileRepository;
import com.wgx.sgcc.model.FileUpload;
import com.wgx.sgcc.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FileServiceImpl implements FileService {
    @Autowired
    private FileRepository fileRepository;

    @Override
    public FileUpload saveFile(FileUpload file) {
        return fileRepository.save(file);
    }

    @Override
    public void removeFile(String id) {
        fileRepository.deleteById(id);
    }

    @Override
    public Optional<FileUpload> getFileById(String id) {
        return fileRepository.findById(id);
    }

    @Override
    public List<FileUpload> listFilesByPage(int pageIndex, int pageSize) {
        Page<FileUpload> page = null;
        List<FileUpload> list = null;
        Sort sort = Sort.by(Sort.Direction.DESC,"uploadDate");
        Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);
        page = fileRepository.findAll(pageable);
        list = page.getContent();
        return list;
    }
}
