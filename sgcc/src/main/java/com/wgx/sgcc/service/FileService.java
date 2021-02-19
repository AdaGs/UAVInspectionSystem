package com.wgx.sgcc.service;


import com.wgx.sgcc.model.FileUpload;

import java.util.List;
import java.util.Optional;

public interface  FileService {
    /**
     * 保存文件
     */
    FileUpload saveFile(FileUpload file);

    /**
     * 删除文件
     */
    void removeFile(String id);

    /**
     * 根据id获取文件
     */
    Optional<FileUpload> getFileById(String id);

    /**
     * 分页查询，
     * @return
     */
    List<FileUpload> listFilesByPage(int pageIndex, int pageSize);
}
