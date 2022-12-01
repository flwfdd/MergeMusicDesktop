package xyz.flwfdd.mergemusicdesktop.model;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author flwfdd
 * @version 1.0
 * @date 2022/11/30 20:54
 * @implNote 文件保存模块
 */
public class Saver {
    final static int buffSize=102400;

    public static boolean download(String src,String path){
        try{
            URL url=new URL(src);
            URLConnection con=url.openConnection();
            InputStream is=con.getInputStream();
            byte[]bs=new byte[buffSize];
            File file=new File(path);
            FileOutputStream os=new FileOutputStream(file,true);
            int len;
            while((len=is.read(bs))!=-1){
                os.write(bs,0,len);
            }
            os.close();
            is.close();
        } catch (Exception e) {
            System.out.println("Download error:"+e);
            return false;
        }
        return true;
    }
}
