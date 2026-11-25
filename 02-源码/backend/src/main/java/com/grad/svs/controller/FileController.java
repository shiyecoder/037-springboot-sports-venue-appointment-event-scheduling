package com.grad.svs.controller;

import com.grad.svs.common.R;
import com.grad.svs.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传接口：图片、报价 Excel、验收照片统一走此入口
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "type", defaultValue = "files") String type) {
        return R.ok(fileService.upload(file, type));
    }
}
