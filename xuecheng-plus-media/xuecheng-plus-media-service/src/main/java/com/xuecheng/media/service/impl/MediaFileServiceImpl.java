package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.config.MinioConfig;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.RestResponse;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MinioClient minioClient;
    @Value("${minio.bucket.files}")
    private String bucket_files;
    @Value("${minio.bucket.videofiles}")
    private String bucket_viedofiles;
    @Autowired
    private MediaFileService currentproxy;
    @Autowired
    private MediaProcessMapper mapperProcessMapper;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, byte[] bytes, UploadFileParamsDto dto, String folder, String objectName) {
        String fileMd5 = DigestUtils.md2Hex(bytes);
        if (StringUtils.isEmpty(folder)) {
            folder = getFileFolder(new Date(), true, true, true);
        } else if (folder.indexOf("/") < 0) {
            folder = folder + "/";
        }
        String filename = dto.getFilename();
        if (StringUtils.isEmpty(objectName)) {
            objectName = fileMd5 + filename.substring(filename.lastIndexOf("."));
        }
        objectName = folder + objectName;
        try {
            //保存文件
            addMediaFilesToMinIO(bytes, bucket_files, objectName);
            //保存到数据库
            MediaFiles mediaFiles = currentproxy.addMediaFilesToDb(companyId, fileMd5, dto, bucket_files, objectName);
            UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
            BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);
            return uploadFileResultDto;
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("上传文件失败");
        }
        return null;
    }


    /**
     * @param bytes      文件字节数组
     * @param bucket     桶
     * @param objectName 对象名称
     * @return void
     * @description 将文件写入minIO
     * @author Mr.M
     * @date 2022/10/12 21:22
     */
    public void addMediaFilesToMinIO(byte[] bytes, String bucket, String objectName) {
        //转为流
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        //扩展名
        String extension = null;
        if (objectName.indexOf(".") >= 0) {
            //文件扩展名
            extension = objectName.substring(objectName.lastIndexOf("."));
        }
        String contentType = getMimeTypeByExtension(extension);
        try {
            PutObjectArgs putObjectArgs = PutObjectArgs.builder().bucket(bucket).object(objectName)
                    //-1表示文件分片按5M(不小于5M,不大于5T),分片数量最大10000，
                    .stream(byteArrayInputStream, byteArrayInputStream.available(), -1)
                    .contentType(contentType)
                    .build();

            minioClient.putObject(putObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            XueChengPlusException.cast("上传文件到文件系统出错");
        }
    }

    private String getMimeTypeByExtension(String extension) {
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (StringUtils.isNotEmpty(extension)) {
            ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
            if (extensionMatch != null) {
                contentType = extensionMatch.getMimeType();
            }
        }
        return contentType;

    }

    /**
     * @param companyId           机构id
     * @param fileMd5             文件md5值
     * @param uploadFileParamsDto 上传文件的信息
     * @param bucket              桶
     * @param objectName          对象名称
     * @return com.xuecheng.media.model.po.MediaFiles
     * @description 将文件信息添加到文件表
     * @author Mr.M
     * @date 2022/10/12 21:22
     */
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId, String fileMd5, UploadFileParamsDto uploadFileParamsDto, String bucket, String objectName) {
        //根据文件名称取出媒体类型
        //扩展名
        String extension = null;
        if(objectName.indexOf(".")>=0){
            extension = objectName.substring(objectName.lastIndexOf("."));
        }
        //获取扩展名对应的媒体类型
        String contentType = getMimeTypeByExtension(extension);
        //从数据库查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            mediaFiles = new MediaFiles();
            //拷贝基本信息
            BeanUtils.copyProperties(uploadFileParamsDto, mediaFiles);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setCompanyId(companyId);
            //图片及mp4文件设置url
            if(contentType.indexOf("image")>=0 || contentType.indexOf("mp4")>=0){
                mediaFiles.setUrl("/" + bucket + "/" + objectName);
            }
            mediaFiles.setBucket(bucket);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");
            //保存文件信息到文件表
            int insert = mediaFilesMapper.insert(mediaFiles);
            if (insert < 0) {
                XueChengPlusException.cast("保存文件信息失败");
            }
            //对avi视频填加到待处理任务
            if (contentType.equals("video/x-msvideo")) {
                MediaProcess mediaProcess = new MediaProcess();
                BeanUtils.copyProperties(mediaFiles,mediaProcess);
                mediaProcess.setStatus("1");
                mapperProcessMapper.insert(mediaProcess);
            }
        }
        return mediaFiles;

    }

    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if (mediaFiles == null) {
            return RestResponse.success(false);
        }
        GetObjectArgs builder = GetObjectArgs.builder().bucket(mediaFiles.getBucket()).object(mediaFiles.getFilePath()).build();
        try {
            minioClient.getObject(builder);
        } catch (Exception e) {
            e.printStackTrace();
            return RestResponse.success(false);
        }
        return RestResponse.success(true);
    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
        //得到分块文件的目录路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块文件的路径
        String chunkFilePath = chunkFileFolderPath + chunkIndex;
        GetObjectArgs builder = GetObjectArgs.builder().bucket(bucket_viedofiles).object(chunkFilePath).build();


        try {
            InputStream inputStream = minioClient.getObject(builder);
            if (inputStream == null) {
                return RestResponse.success(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RestResponse.success(false);
        }
        return RestResponse.success(true);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, byte[] bytes) {

        //得到分块文件的目录路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        //得到分块文件的路径
        String chunkFilePath = chunkFileFolderPath + chunk;

        try {
            //将文件存储至minIO
            addMediaFilesToMinIO(bytes, bucket_viedofiles, chunkFilePath);
            return RestResponse.success(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.debug("上传分块文件:{},失败:{}", chunkFilePath, ex.getMessage());
        }
        return RestResponse.validfail(false, "上传分块失败");
    }

    @Override
    public RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamsDto uploadFileParamsDto) {
        //1.下载所有分块文件
        File[] files = checkChunkStatus(fileMd5, chunkTotal);
        String fileName = uploadFileParamsDto.getFilename();
        //扩展名
        String extName = fileName.substring(fileName.lastIndexOf("."));
        //2.创建临时文件作为合并文件
        File mergeFile = null;
        try {
            mergeFile = File.createTempFile(fileMd5, extName);
        } catch (IOException e) {
            XueChengPlusException.cast("合并文件过程中创建临时文件出错");
        }
        try {
            //3.开始合并
            byte[] b = new byte[1024];
            try (RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");) {
                for (File chunkFile : files) {
                    try (FileInputStream chunkFileStream = new FileInputStream(chunkFile);) {
                        int len = -1;
                        while ((len = chunkFileStream.read(b)) != -1) {
                            //向合并后的文件写
                            raf_write.write(b, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                XueChengPlusException.cast("合并文件过程中出错");
            }
            log.debug("合并文件完成{}", mergeFile.getAbsolutePath());
            uploadFileParamsDto.setFileSize(mergeFile.length());
            //4.合并文件校验
            try (InputStream mergeFileInputStream = new FileInputStream(mergeFile);) {
                //对文件进行校验，通过比较md5值
                String newFileMd5 = DigestUtils.md5Hex(mergeFileInputStream);
                if (!fileMd5.equalsIgnoreCase(newFileMd5)) {
                    //校验失败
                    XueChengPlusException.cast("合并文件校验失败");
                }
                log.debug("合并文件校验通过{}", mergeFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                //校验失败
                XueChengPlusException.cast("合并文件校验异常");
            }
            //5.将临时文件上传至minio
            String mergeFilePath = getFilePathByMd5(fileMd5, extName);
            try {
                //上传文件到minIO
                addMediaFilesToMinIO(mergeFile.getAbsolutePath(), bucket_viedofiles, mergeFilePath);
                log.debug("合并文件上传MinIO完成{}", mergeFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                XueChengPlusException.cast("合并文件时上传文件出错");
            }
            //6.入数据库
            MediaFiles mediaFiles = currentproxy.addMediaFilesToDb(companyId, fileMd5, uploadFileParamsDto, bucket_files, mergeFilePath);
            if (mediaFiles == null) {
                XueChengPlusException.cast("媒资文件入库出错");
            }
            return RestResponse.success();
        } finally {
            //删除临时文件
            for (File file : files) {
                try {
                    file.delete();
                } catch (Exception e) {

                }
            }
            try {
                mergeFile.delete();
            } catch (Exception e) {

            }
        }
    }

    @Override
    public MediaFiles getFileById(String id) {
        return mediaFilesMapper.selectById(id);
    }

    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }

    //将合并的文件上传到minIO，传入文件绝对路径
    public void addMediaFilesToMinIO(String filePath, String bucket, String objectName) {
        //扩展名
        String extension = null;
        if (objectName.indexOf(".") >= 0) {
            extension = objectName.substring(objectName.lastIndexOf("."));
        }
        //获取扩展名对应的媒体类型
        String contentType = getMimeTypeByExtension(extension);
        try {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .filename(filePath)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            e.printStackTrace();
            XueChengPlusException.cast("上传文件到文件系统出错");
        }
    }

    //检查所有分块是否上传完毕
    private File[] checkChunkStatus(String fileMd5, int chunkTotal) {
        //得到分块文件的目录路径
        String chunkFileFolderPath = getChunkFileFolderPath(fileMd5);
        File[] files = new File[chunkTotal];
        for (int i = 0; i < chunkTotal; i++) {
            String chunFilePath = chunkFileFolderPath + i;
            //下载文件
            File chunkFile = null;
            try {
                chunkFile = File.createTempFile("chunk" + i, null);
            } catch (IOException e) {
                e.printStackTrace();
                XueChengPlusException.cast("下载分块时创建临时文件出错");
            }
            chunkFile = downloadFileFromMinIO(chunkFile, bucket_viedofiles, chunFilePath);
            files[i] = chunkFile;
        }
        return files;
    }

    //根据桶和文件路径从minio下载文件
    public File downloadFileFromMinIO(File file, String bucket, String objectName) {
        InputStream fileInputStream = null;
        OutputStream fileOutputStream = null;
        try {
            fileInputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build());
            try {
                fileOutputStream = new FileOutputStream(file);
                IOUtils.copy(fileInputStream, fileOutputStream);

            } catch (IOException e) {
                XueChengPlusException.cast("下载文件" + objectName + "出错");
            }
        } catch (Exception e) {
            e.printStackTrace();
            XueChengPlusException.cast("文件不存在" + objectName);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    //得到分块文件的目录
    private String getChunkFileFolderPath(String fileMd5) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + "chunk" + "/";
    }

    //根据日期拼接目录
    private String getFileFolder(Date date, boolean year, boolean month, boolean day) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //获取当前日期字符串
        String dateString = sdf.format(new Date());
        //取出年、月、日
        String[] dateStringArray = dateString.split("-");
        StringBuffer folderString = new StringBuffer();
        if (year) {
            folderString.append(dateStringArray[0]);
            folderString.append("/");
        }
        if (month) {
            folderString.append(dateStringArray[1]);
            folderString.append("/");
        }
        if (day) {
            folderString.append(dateStringArray[2]);
            folderString.append("/");
        }
        return folderString.toString();
    }


}
