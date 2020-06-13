package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@Slf4j
@Service
@EnableConfigurationProperties(UploadProperties.class)
public class UploadService {
    // 可接受的图片类型
    //private static final List<String> allowType = Arrays.asList("image/jpeg", "image/png", "image/bmp");

    @Autowired
    private UploadProperties pros;

    @Autowired
    private FastFileStorageClient storageClient;

    public String uploadImage(MultipartFile file) {
        try {
            // 校验文件类型
            String contentType = file.getContentType();
            if(!pros.getAllowTypes().contains(contentType)){
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }
            // 校验图片内容
            BufferedImage image = ImageIO.read(file.getInputStream());
            if(image==null){
                throw new LyException(ExceptionEnum.INVALID_FILE_TYPE);
            }

            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            StorePath storePath = storageClient.uploadFile(file.getInputStream(), file.getSize(), extension, null);

            return pros.getBaseUrl()+storePath.getFullPath();


//            // 准备文件上传路径
//            File dest = new File("C:\\Users\\Lenovo\\Desktop\\master\\spring_zg2_demo\\upload", file.getOriginalFilename());
//            // 保存文件
//            file.transferTo(dest);
            // 返回路径
//            return "http://image.leyou.com/" + file.getOriginalFilename();
        } catch (IOException e) {
            // 上传失败
            log.error("【文件上传】 文件上传失败！",e);
            throw new LyException(ExceptionEnum.UPLOAD_FILE_ERROR);
        }

    }
}
