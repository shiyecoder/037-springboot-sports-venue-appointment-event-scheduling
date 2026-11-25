package com.grad.svs.service;

import com.grad.svs.common.BizException;
import com.grad.svs.common.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传服务：统一落盘到项目根目录 uploads/，数据库只保存相对路径。
 * 禁止前端提交外部 URL，扩展名与大小在此处再做一次服务端校验。
 */
@Service
public class FileService {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private static final List<String> IMAGE_EXT = List.of("jpg", "jpeg", "png", "webp");
    private static final List<String> FILE_EXT = List.of("pdf", "xlsx", "xls", "csv", "doc", "docx");

    public Map<String, Object> upload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        boolean isImage = IMAGE_EXT.contains(ext);
        if (!isImage && !FILE_EXT.contains(ext)) {
            throw new BizException("不支持的文件类型：" + ext);
        }
        long limit = isImage ? 5L * 1024 * 1024 : 20L * 1024 * 1024;
        if (file.getSize() > limit) {
            throw new BizException(isImage ? "图片不能超过 5MB" : "附件不能超过 20MB");
        }
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String dir = (type == null || type.isBlank() ? "files" : type) + "/" + month;
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path target = Paths.get(uploadDir, dir).toAbsolutePath().normalize();
            Files.createDirectories(target);
            file.transferTo(target.resolve(name).toFile());
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("path", "/uploads/" + dir + "/" + name);
        out.put("name", original);
        out.put("size", file.getSize());
        out.put("uploaderId", UserContext.userId());
        return out;
    }
}
