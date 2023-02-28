package com.xuecheng.media;

import io.minio.*;
import io.minio.errors.MinioException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @Description:
 * @author: 刘
 * @date: 2023年02月28日 下午 4:18
 */
public class MinioTest {
    static MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://127.0.0.1:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();
    //上传文件
    public static void upload()throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        try {
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket("testbucket").build());
            //检查testbucket桶是否创建，没有创建自动创建
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("testbucket").build());
            } else {
                System.out.println("Bucket 'testbucket' already exists.");
            }
            //上传1.mp4
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("testbucket")
                            .object("笔记.doc")
                            .filename("D:\\develop\\upload\\笔记.doc")
                            .build());
            //上传1.avi,上传到avi子目录
            //minioClient.uploadObject(
            //        UploadObjectArgs.builder()
            //                .bucket("testbucket")
            //                .object("avi/1.avi")
            //                .filename("D:\\develop\\upload\\1.avi")
            //                .build());
            //System.out.println("上传成功");
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }

    }
    //删除文件
    public static void delete(String bucket,String filepath)throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        try {

            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(filepath).build());
            System.out.println("删除成功");
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }

    }
    public static void main(String[] args)throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        //upload();
        delete("testbucket","笔记.doc");
        delete("testbucket","笔记.doc");
    }


}
