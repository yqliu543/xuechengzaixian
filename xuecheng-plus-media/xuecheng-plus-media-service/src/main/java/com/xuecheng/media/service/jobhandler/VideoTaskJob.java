package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xuecheng.media.service.impl.MediaFileServiceImpl;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @author: 刘
 * @date: 2023年03月02日 下午 2:12
 */
@Component
@Slf4j
public class VideoTaskJob {
    private static Logger logger = LoggerFactory.getLogger(SampleXxlJob.class);
    @Autowired
    private MediaFileProcessService mediaFileProcessService;
    @Autowired
    private MediaFileService mediaFileService;
    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;


    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("分片参数：当前分片序号 = {}, 总分片数 = {}", shardIndex, shardTotal);

        // 查询待处理任务
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, 2);
        if (mediaProcessList == null || mediaProcessList.size() < 1) {
            log.debug("没有待处理视频");
            return;
        }
        int size = mediaProcessList.size();

        // 启动多线程处理
        //创建size个线程池
        ExecutorService threadPool = Executors.newFixedThreadPool(size);
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            threadPool.execute(() -> {
                //任务执行逻辑
                String status = mediaProcess.getStatus();
                if ("2".equals(status)) {
                    log.debug("视频已经处理");
                    countDownLatch.countDown();
                    return;
                }
                //桶
                String bucket = mediaProcess.getBucket();
                //存储路径
                String filePath = mediaProcess.getFilePath();
                //原始视频的md5值
                String fileId = mediaProcess.getFileId();
                //原始文件名称
                String filename = mediaProcess.getFilename();

                //将要处理的文件下载到服务器上
                File originalFile = null;
                //处理结束的视频文件
                File mp4File = null;

                try {
                    originalFile = File.createTempFile("original", null);
                    mp4File = File.createTempFile("mp4", ".mp4");
                } catch (IOException e) {
                    log.error("处理视频前创建临时文件失败");
                    countDownLatch.countDown();
                    return;
                }
                // 将原视频下载到本地
                try {
                    mediaFileService.downloadFileFromMinIO(originalFile, bucket, filePath);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("下载过程出错");
                }

                // 调用工具转成mp4
                //转换后mp4文件的名称
                String mp4_name = fileId + ".mp4";
                //转换后mp4文件的路径
                String mp4_path = mp4File.getAbsolutePath();
                //创建工具类对象
                Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, originalFile.getAbsolutePath(), mp4_name, mp4_path);
                //开始视频转换，成功将返回success
                String result = videoUtil.generateMp4();
                String statusNew = "3";
                String url = null;
                if (result.equals("success")) {
                    String filePathByMd5 = getFilePathByMd5(fileId, ".mp4");
                    // 上传到minIO
                    try {
                        mediaFileService.addMediaFilesToMinIO(mp4_path, bucket, filePathByMd5);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("上传出错");
                        countDownLatch.countDown();
                        return;
                    }
                    statusNew = "2";
                    url = "/" + bucket + "/" + filePathByMd5;
                }
                // 记录任务
                try {
                    mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), statusNew, fileId, url, result);
                } catch (Exception e) {
                    e.printStackTrace();
                    countDownLatch.countDown();
                    return;
                }
                //计数器减一
                countDownLatch.countDown();
            });

        });
        //阻塞解除
        countDownLatch.await(30, TimeUnit.MINUTES);


    }

    private String getFilePathByMd5(String fileMd5, String fileExt) {
        return fileMd5.substring(0, 1) + "/" + fileMd5.substring(1, 2) + "/" + fileMd5 + "/" + fileMd5 + fileExt;
    }
}
