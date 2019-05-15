package com.zjxt.fileupload.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zjxt.fileupload.constant.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author: heiheihaxi
 * @Date: 2019/5/5 11:02
 */

@RestController
@RequestMapping("/file")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 获取需要存储文件再目标目录是否存在，返回存在文件的大小，不存在，返回-1
    @RequestMapping("/upload/getExistFileSize")
    public void getExistFileSize(HttpServletRequest request, HttpServletResponse response){
        //存储文件的路径
        String currentFilePath = Constant.FILE_UPLOAD_PATH;
        PrintWriter print = null;
        try {
            request.setCharacterEncoding("utf-8");
            print = response.getWriter();
            String fileName = new String(request.getParameter("fileName").getBytes("ISO-8859-1"),"UTF-8");
            String lastModifyTime = request.getParameter("lastModifyTime");
            File file = new File(currentFilePath+fileName+"."+lastModifyTime);
            if(file.exists()){
                print.print(file.length());
            }else{
                print.print(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @RequestMapping("/upload/appendFile")
    public static void appendFile(HttpServletRequest request,HttpServletResponse response) {
        PrintWriter print = null;
        try {
            // 设置编码级别
            request.setCharacterEncoding("utf-8");
            print = response.getWriter();
            // 获取属性 fileSize文件大小
            String fileSize = request.getParameter("fileSize");
            long totalSize = Long.valueOf(fileSize);
            RandomAccessFile randomAccessfile = null;
            long currentFileLength = 0;// 记录当前文件大小，用于判断文件是否上传完成
            String currentFilePath = Constant.FILE_UPLOAD_PATH;// 记录当前文件的绝对路径
//            String fileName = new String(request.getParameter("fileName").getBytes("ISO-8859-1"),"UTF-8");
            // 获取属性 文件名，解决乱码
            String fileName = new String(request.getParameter("fileName").getBytes("UTF-8"));
            // 获取属性 lastModifyTime 上次更改时间
            String lastModifyTime = request.getParameter("lastModifyTime");
            // 创建临时文件对象
            File file = new File(currentFilePath+fileName+"."+lastModifyTime);
            // 存在文件
            if(file.exists()){
                randomAccessfile = new RandomAccessFile(file, "rw");
            }
            else {
                // 不存在文件，根据文件标识创建文件
                randomAccessfile = new RandomAccessFile(currentFilePath+fileName+"."+lastModifyTime, "rw");
            }
            // 开始文件传输
            InputStream in = request.getInputStream();
            randomAccessfile.seek(randomAccessfile.length());
            byte b[] = new byte[1024];
            int n;
            while ((n = in.read(b)) != -1) {
//                System.out.println(new String(b,"utf-8"));
                randomAccessfile.write(b, 0, n);
            }

            currentFileLength = randomAccessfile.length();

            // 关闭文件
            closeRandomAccessFile(randomAccessfile);
            randomAccessfile = null;
            // 整个文件上传完成,修改文件后缀
            if (currentFileLength == totalSize) {
                File oldFile = new File(currentFilePath+fileName+"."+lastModifyTime);
                File newFile = new File(currentFilePath+fileName);
                if(!oldFile.exists()){
                    return;//重命名文件不存在
                }
                if(newFile.exists()){// 如果存在形如test.txt的文件，则新的文件存储为test+当前时间戳.txt, 没处理不带扩展名的文件
                    String newName = fileName.substring(0,fileName.lastIndexOf("."))
                            +"-"+sdf.format(new Date())+"."
                            +fileName.substring(fileName.lastIndexOf(".")+1);
                    newFile = new File(currentFilePath+newName);
                }
                if(!oldFile.renameTo(newFile)){
                    oldFile.delete();
                }

            }
            print.print(currentFileLength);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获取所有的json文件
    @RequestMapping("/allJsons")
    public JSON getJsons(){
        File file = new File(Constant.FILE_FINISH_PATH);
        JSONObject object = new JSONObject();

        JSONArray jsonArray=new JSONArray();

        File[] files = file.listFiles();
        if (null != files)
        for (File f: files) {
            String fileName = f.getName();
            if (fileName.endsWith(".meta")){
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("fileName",fileName);
                jsonArray.add(jsonObject);
            }
        }
        object.put("filelists",jsonArray);
        return object;
    }

    // 获取所有的json文件
    @RequestMapping("/getAllJsons")
    public JSON getAllJsons(){
        File file = new File(Constant.FILE_FINISH_PATH);
        JSONObject object = new JSONObject();

        List<String> lists = new ArrayList<>();

        File[] files = file.listFiles();
        if (null != files)
            for (File f: files) {
                String fileName = f.getName();
                if (fileName.endsWith(".meta")){
                    lists.add(fileName);
                }
            }
        object.put("filelists",lists);
        return object;
    }

    // 根据文件名获取json文件
    @RequestMapping("/getJsonsFile")
    public String getJsonsFile(@RequestParam String fileName){
        File file = new File(Constant.FILE_FINISH_PATH+fileName);
        StringBuilder sb=new StringBuilder();
        try {
            BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
            String line;
            while (true){
                line=br.readLine();
                if (null == line){
                    break;
                }
                sb.append(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    // 根据文件名获取json文件
    @RequestMapping("/getJsonFiles")
    public JSON getJsonFiles(){
        File file = new File(Constant.FILE_FINISH_PATH);
        File[] files = file.listFiles();
        JSONObject jsonObject = new JSONObject();
        try {
            if (null != files)
            for (File f : files){
                String fileName = f.getName();
                // 如果是json文件
                if (fileName.endsWith(".meta")){
                    StringBuilder sb=new StringBuilder();
                    BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(f),"utf-8"));
                    String line;
                    while (true){
                        line=br.readLine();
                        if (null == line){
                            break;
                        }
                        sb.append(line);
                    }
                    br.close();
                    jsonObject.put(fileName,sb.toString());

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


    // 根据文件名获取json文件
    @RequestMapping("/getJsonsFiles")
    public JSON getJsonsFiles(){
        File file = new File(Constant.FILE_FINISH_PATH);
        File[] files = file.listFiles();

        JSONObject object = new JSONObject();
        JSONArray jsonArray=new JSONArray();
        try {
            if (null != files)
            for (File f : files){
                String fileName = f.getName();
                // 如果是json文件
                if (fileName.endsWith(".meta")){
                    JSONObject jsonObject = new JSONObject();
                    StringBuilder sb=new StringBuilder();
                    BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(f),"utf-8"));
                    String line;
                    while (true){
                        line=br.readLine();
                        if (null == line){
                            break;
                        }
                        sb.append(line);
                    }
                    br.close();
                    jsonObject.put("fileName",fileName);
                    jsonObject.put("content",JSONObject.parse(sb.toString()));
                    jsonArray.add(jsonObject);
                }

            }

            object.put("allFiles",jsonArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return object;
    }


    // 根据文件名删除json文件
    @RequestMapping("/delJsonsFile")
    public String delJsonsFile(@RequestParam String fileName){
        File file = new File(Constant.FILE_FINISH_PATH+fileName);
        boolean delete = file.delete();
        if (delete){
            return "删除文件 "+ fileName +"成功";
        }else {
            return "删除文件 "+ fileName +"失败";
        }
    }

    // 根据文件名删除已存入数据库的meta文件
    @RequestMapping("/delMetaExistFile")
    public void delMetaExistFile(){

        String sql="select uuid from t_956ZH21OATECB0001";

        List<String> list = jdbcTemplate.queryForList(sql, String.class);

        File file = new File(Constant.FILE_FINISH_PATH);

        File[] files = file.listFiles();

        String fileName="";

        if ( null != files){
            for (File f : files){
                fileName = f.getName();
                fileName = fileName.substring(0,fileName.lastIndexOf("."));
                if (list.contains(fileName)){
                    System.out.println("存在文件"+fileName);
                    boolean delete = f.delete();
                    if (delete){
                        logger.info("删除文件 "+ fileName +"成功");
                    }else {
                        logger.error("删除文件 "+ fileName +"失败");
                    }
                }
            }

        }


    }



    /**
     * 关闭随机访问文件
     */
    private static void closeRandomAccessFile(RandomAccessFile rfile) {
        if (null != rfile) {
            try {
                rfile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String getFileSize(Long l){
        if (l>(1024*1024*1024)){
            return Math.round(l * 100 / (1024 * 1024 * 1024))/100.0f+"GB";
        }else if (l>(1024*1024)){
            return Math.round(l * 100 / (1024 * 1024))/100.0f+"MB";
        }else if (l>1024){
            return Math.round(l * 100 / 1024)/100.0f+"KB";
        }else {
            return l+"B";
        }

    }


}
