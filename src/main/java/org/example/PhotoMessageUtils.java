package org.example;

import functions.FilterOperations;
import utils.ImageUtils;
import utils.RgbMaster;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;

public class PhotoMessageUtils {
    public static List<String> savePhotos(List<org.telegram.telegrambots.meta.api.objects.File> files, String botToken) throws Exception {
        Random random = new Random();
        ArrayList<String> paths = new ArrayList<>();
        for (org.telegram.telegrambots.meta.api.objects.File file : files) {
            final String imagesUrl = "https://api.telegram.org/file/bot" + botToken + "/" + file.getFilePath();
            final String localFileName = "images/" + new Date().getTime() + random.nextLong() + ".png";
            saveImage(imagesUrl, localFileName);
            paths.add(localFileName);
        }
        return paths;
    }
    public static void saveImage(String url, String filename) throws Exception{
        URL urlModel = new URL(url);
        InputStream inputStream = urlModel.openStream();
        OutputStream outputStream = new FileOutputStream(filename);
        byte[] b = new byte[2048];
        int len;
        while ((len = inputStream.read(b)) != -1) {
            outputStream.write(b, 0, len);
        }
        inputStream.close();
        outputStream.close();
    }
    public static void processingImage(String fileName) throws Exception {
        final BufferedImage image = ImageUtils.getImage(fileName);
        final RgbMaster rgbMaster = new RgbMaster(image);
        rgbMaster.changeImage(FilterOperations::grayScale);
        ImageUtils.saveImage(rgbMaster.getImage(), fileName);
    }
}
