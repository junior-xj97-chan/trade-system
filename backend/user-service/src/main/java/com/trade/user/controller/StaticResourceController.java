package com.trade.user.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * 静态资源控制器 - 提供上传文件的访问
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads";

    /**
     * 获取上传的 avatar 图片
     * 访问路径：/static/avatars/{filename}
     */
    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        File file = new File(UPLOAD_DIR + File.separator + "avatars" + File.separator + filename);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        // 根据文件扩展名设置 Content-Type
        String contentType = getContentType(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private String getContentType(String filename) {
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }
}
