package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.config.MinioConfig;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/9/10 8:58
 * @version 1.0
 */
@Slf4j
 @Service
public class MediaFileServiceImpl implements MediaFileService {

  @Autowired
  MediaFilesMapper mediaFilesMapper;
  @Autowired
  MinioClient minioClient;
  @Value("${minio.bucket.files}")
  private  String bucket_files;

 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

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
        if (StringUtils.isEmpty(folder)){
            folder = getFileFolder(new Date(), true, true, true);
        }else if (folder.indexOf("/")<0){
            folder=folder+"/";
        }
        String filename = dto.getFilename();
        if (StringUtils.isEmpty(objectName)){
            objectName=fileMd5+filename.substring(filename.lastIndexOf("."));
        }
        objectName=folder+objectName;
        try {
            String contentType = dto.getContentType();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket_files)
                    .object(objectName)
                    .stream(byteArrayInputStream, byteArrayInputStream.available(), -1)
                    .contentType(contentType).build();
            minioClient.putObject(putObjectArgs);
            //保存到数据库
            MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
            if (mediaFiles==null){
                mediaFiles=new MediaFiles();
                BeanUtils.copyProperties(dto,mediaFiles);
//                mediaFiles.setId();
                mediaFiles.setFileId(fileMd5);
                mediaFiles.setCompanyId(companyId);
                mediaFiles.setBucket(bucket_files);
                mediaFiles.setFilename(filename);
                mediaFiles.setFilePath(objectName);
                mediaFiles.setUrl("/"+bucket_files+"/"+objectName);
                mediaFiles.setCreateDate(LocalDateTime.now());
                mediaFiles.setStatus("1");
                mediaFiles.setAuditStatus("002003");
                mediaFilesMapper.insert(mediaFiles);
            }
            UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
            BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
            return uploadFileResultDto;
        } catch (Exception e) {
            e.printStackTrace();
            log.debug("上传文件失败");
        }
        return null;
    }
    //根据日期拼接目录
    private String getFileFolder(Date date, boolean year, boolean month, boolean day){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //获取当前日期字符串
        String dateString = sdf.format(new Date());
        //取出年、月、日
        String[] dateStringArray = dateString.split("-");
        StringBuffer folderString = new StringBuffer();
        if(year){
            folderString.append(dateStringArray[0]);
            folderString.append("/");
        }
        if(month){
            folderString.append(dateStringArray[1]);
            folderString.append("/");
        }
        if(day){
            folderString.append(dateStringArray[2]);
            folderString.append("/");
        }
        return folderString.toString();
    }
}
