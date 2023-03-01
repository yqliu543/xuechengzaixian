package com.xuecheng.media;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.*;
import java.util.*;

/**
 * @Description:
 * @author: 刘
 * @date: 2023年03月01日 下午 1:41
 */
public class BigFileTest {
    @Test
    public void doupload() throws IOException {
        File file = new File("E:\\西安北客站\\09 供水、供电、供气突发事故专项应急 9-24.mp4");
        File folderPath = new File("D:\\develop\\bigfile_data\\");
        if (!folderPath.exists()){
            folderPath.mkdirs();
        }
        int chunSize=1024*1024*1;
        long number = (long) Math.ceil(file.length() * 1.0 / chunSize);
        //使用流对象读取源文件，向分块文件写数据，达到分块大小不再写
        RandomAccessFile r = new RandomAccessFile(file, "r");
        //缓冲区
        byte[] b=new byte[1024];
        for (int i = 0; i < number; i++) {
            File file1 = new File("D:\\develop\\bigfile_data\\" + i);
            boolean newFile = file1.createNewFile();
            if (newFile){
                //向分块文件写数据的流对象
                RandomAccessFile rw = new RandomAccessFile(file1, "rw");
                int len=-1;
                while ((len=r.read(b))!=-1){
                    rw.write(b,0,len);
                    if (file1.length()>=chunSize){
                        break;
                    }
                }
                rw.close();
            }
        }
        r.close();
    }

    @Test
    public void hebing() throws IOException {
        File file = new File("E:\\西安北客站\\09 供水、供电、供气突发事故专项应急 9-24.mp4");
        File folderPath = new File("D:\\develop\\bigfile_data\\");
        if (!folderPath.exists()){
            folderPath.mkdirs();
        }
        //合并后的文件
        File mergeFile = new File("D:\\develop\\bigfile_data\\09 供水、供电、供气突发事故专项应急 9-24.mp4");

        File[] chunfiles = folderPath.listFiles();
        List<File> chunfileList = Arrays.asList(chunfiles);
        //按文件名升序排序
        Collections.sort(chunfileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });
        RandomAccessFile rw = new RandomAccessFile(mergeFile, "rw");
        byte[] b=new byte[1024];
        for (File it:chunfileList){
            RandomAccessFile r = new RandomAccessFile(it, "r");
            int len=-1;
            while ((len=r.read(b))!=-1){
                rw.write(b,0,len);
            }
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        FileInputStream mergeFileInputStream = new FileInputStream(mergeFile);
        String sourceMd5 = DigestUtils.md5Hex(fileInputStream);
        String mergeMd5 = DigestUtils.md5Hex(mergeFileInputStream);
        if (sourceMd5.equals(mergeMd5)){
            System.out.println("合并成功");
        }

    }
}
