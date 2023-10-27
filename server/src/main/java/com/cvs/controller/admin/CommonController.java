package com.cvs.controller.admin;

import com.cvs.constant.MessageConstant;
import com.cvs.result.Result;
import com.cvs.utils.S3OssUtil;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Api(tags = "通用接口")
@Slf4j
public class CommonController {
    @Autowired
    private S3OssUtil s3OssUtil;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}",file);

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objName = UUID.randomUUID().toString() + extension;
            String uploadUrl = s3OssUtil.upload(file.getBytes(), objName);
            return Result.success(uploadUrl);
        } catch (IOException e) {
            log.error("文件上传失败：{}",e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
