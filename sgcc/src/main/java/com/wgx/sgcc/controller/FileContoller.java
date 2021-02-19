package com.wgx.sgcc.controller;

import com.wgx.sgcc.config.JsonResult;
import com.wgx.sgcc.model.FileUpload;
import com.wgx.sgcc.service.FileService;
import com.wgx.sgcc.util.MD5Util;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @time 2021-2-18
 * @author gs
 */
@Api(description = "文件管理")
@RestController
@RequestMapping("/api/file")
public class FileContoller {

    private static final Logger logger = LoggerFactory.getLogger(FileContoller.class);

   @Autowired
   private FileService fileService;


    @Value("${server.port}")
    private String serverPort;

    @RequestMapping(value = "/")
    public String index(Model model) {
        // 展示最新二十条数据
        model.addAttribute("files", fileService.listFilesByPage(0, 20));
        return "index";
    }

    /**
     * 分页查询文件
     */
    @GetMapping("files/{pageIndex}/{pageSize}")
    @ResponseBody
    public List<FileUpload> listFilesByPage(@PathVariable int pageIndex, @PathVariable int pageSize) {
        return fileService.listFilesByPage(pageIndex, pageSize);
    }

    /**
     * 获取文件片信息
     */
    @GetMapping("files/{id}")
    @ApiOperation("文件查看")
    @ResponseBody
    public ResponseEntity<Object> serveFile(@RequestParam("id") String id) throws UnsupportedEncodingException {

        Optional<FileUpload> file = fileService.getFileById(id);

        if (file.isPresent()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; fileName=" + new String(file.get().getName().getBytes("utf-8"),"ISO-8859-1"))
                    .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                    .header(HttpHeaders.CONTENT_LENGTH, file.get().getSize() + "").header("Connection", "close")
                    .body(file.get().getContent().getData());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File was not fount");
        }

    }

    /**
     * 在线显示文件
     */
    @GetMapping("/view")
    @ApiOperation("文件显示")
    @ResponseBody
    public ResponseEntity<Object> serveFileOnline(@RequestParam("id") String id) {

        Optional<FileUpload> file = fileService.getFileById(id);

        if (file.isPresent()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "fileName=\"" + file.get().getName() + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, file.get().getContentType())
                    .header(HttpHeaders.CONTENT_LENGTH, file.get().getSize() + "").header("Connection", "close")
                    .body(file.get().getContent().getData());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File was not fount");
        }

    }

    /**
     * 上传
     */
    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {

        try {
            FileUpload f = new FileUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    new Binary(file.getBytes()));
                f.setMd5(MD5Util.getMD5(file.getInputStream()));
            fileService.saveFile(f);
            System.out.println(f);
        } catch (IOException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "Your " + file.getOriginalFilename() + " is wrong!");
            return "redirect:/";
        }

        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }

    /**
     * 上传接口
     */
    @ApiOperation("文件上传")
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<String> handleFileUpload(@RequestParam(value="file",required=true) MultipartFile file) {
        FileUpload returnFile = null;
        try {
            FileUpload f = new FileUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(),
                    new Binary(file.getBytes()));
            f.setMd5(MD5Util.getMD5(file.getInputStream()));
            returnFile = fileService.saveFile(f);
            String path = "//"  + serverPort + "/view/" + returnFile.getId();
            return ResponseEntity.status(HttpStatus.OK).body(path);
        } catch (IOException | NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }



    @ApiOperation("文件上传")
    @RequestMapping(value = "/uploadFile",method = RequestMethod.POST)
    public JsonResult uploadFile(@RequestParam("file") @ApiParam(value = "二进制文件流") MultipartFile file){
        String fileName = file.getOriginalFilename();//获取文件名
        fileName = getFileName(fileName);
        String filepath = getUploadPath();
        if (!file.isEmpty()) {
            try (BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(new File(filepath + File.separator + fileName)))) {
                out.write(file.getBytes());
                out.flush();
                  return JsonResult.returnSuccess("成功",fileName);
            } catch (FileNotFoundException e) {
                return JsonResult.error(-1,"找不到文件",null);
            } catch (IOException e) {
                return JsonResult.error(-1,"上传文件失败 IOException：" + e.getMessage(),null);
            }
        } else {
            return JsonResult.error(-1,"上传文件失败，文件为空",null);
        }
    }

    private String getFileName(String fileName) {
        int index = fileName.lastIndexOf(".");
        final SimpleDateFormat sDateFormate = new SimpleDateFormat("yyyymmddHHmmss");  //设置时间格式
        String nowTimeStr = sDateFormate.format(new Date()); // 当前时间
        fileName = fileName.substring(0, index) + "_" + nowTimeStr + fileName.substring(index);
        return fileName;
    }

    private String getUploadPath() {
        File path = null;
        try {
            path = new File(ResourceUtils.getURL("classpath:").getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (!path.exists()) path = new File("");
        File upload = new File(path.getAbsolutePath(), "static/upload/");
        if (!upload.exists()) upload.mkdirs();
        return upload.getAbsolutePath();
    }

}
