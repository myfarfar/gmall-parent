package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("admin/product")
public class FileUploadController {

    //  获取文件上传对应的地址
    @Value("${minio.endpointUrl}")
    public String endpointUrl;

    @Value("${minio.accessKey}")
    public String accessKey;

    @Value("${minio.secreKey}")
    public String secreKey;

    @Value("${minio.bucketName}")
    public String bucketName;

//文件上传控制器
    @PostMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws Exception  {
    //获取上传路径
        String url ="";
        //使用minio的url创建MinioClient对象
        MinioClient minioClient = MinioClient.builder().endpoint(endpointUrl).credentials(accessKey,secreKey).build();
        //检查存储桶是否已经存在
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (exists) {
            System.out.println("Bucket already exists.");
        }else {
            //创建一个名为asiatrip的存储桶，用于存储照片的zip文件。
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        //创建唯一的文件名字
        String fileName = System.currentTimeMillis()+ UUID.randomUUID().toString();
//        使用putObject上传文件的到存储桶
        minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                        file.getInputStream(),file.getSize(),-1).contentType(file.getContentType()).build());

//                文件上传路径返回给页面
        url = endpointUrl + "/" + bucketName + "/" + fileName;
        System.out.println("url:\t = " + url);
        return Result.ok(url);
    }

}
