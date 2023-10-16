package utils;

import commands.AppBotCommands;
import functions.FilterOperations;
import functions.ImagesOperation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.attribute.AclFileAttributeView;

public class ImageUtils {

    public static BufferedImage getImage(String path) throws IOException {
        final File file = new File(path);
        return ImageIO.read(file);
    }

    public static void saveImage(BufferedImage image, String path) throws IOException {
        ImageIO.write(image, "png", new File(path));
    }

    static float[] rgbIntToArray(int pixel) {
        Color color = new Color(pixel);
        return color.getRGBComponents(null);
    }

    static int arrayToRgbInt(float[] pixel) throws Exception {
        Color color = null;
        if (pixel.length == 3) {
            color = new Color(pixel[0], pixel[1], pixel[2]);
        } else if (pixel.length == 4) {
            color = new Color(pixel[0], pixel[1], pixel[2], pixel[3]);
        }
        if (color != null) {
            return color.getRGB();
        }
        throw  new Exception("invalid color");
    }
    public static ImagesOperation getOperation(String operationName) {
        FilterOperations filterOperations = new FilterOperations();
        Method[] methods = filterOperations.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommands.class)) {
                AppBotCommands command = method.getAnnotation(AppBotCommands.class);
                if (command.name().equals(operationName)) {
                    return (f) -> {
                        try {
                            return (float[]) method.invoke(filterOperations, f);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    };
                }
            }
        }
        return null;
    }
}
