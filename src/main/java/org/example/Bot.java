package org.example;


import commands.AppBotCommands;
import commands.BotCommonCommands;
import functions.ImagesOperation;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import utils.ImageUtils;
import utils.RgbMaster;
import functions.FilterOperations;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.annotation.Retention;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static org.example.PhotoMessageUtils.processingImage;


public class Bot extends TelegramLongPollingBot {

    private HashMap<String, Message> messages = new HashMap<>();


    @Override
    public void onUpdateReceived(Update update){
        Message message = update.getMessage();
        try {
            SendMessage responseTextMessage = runCommonCommand(message);
            if (responseTextMessage != null) {
                execute(responseTextMessage);
                return;
            }
            responseTextMessage = runPhotoMessage(message);
            if (responseTextMessage != null) {
                execute(responseTextMessage);
                return;
            }
            Object responseMediaMessage = runPhotoFilter(message);
            if (responseMediaMessage != null) {
                if (responseMediaMessage instanceof SendMediaGroup) {
                    execute((SendMediaGroup) responseMediaMessage);
                } else if (responseMediaMessage instanceof SendMessage){
                    execute((SendMessage) responseMediaMessage);
                }
                return;
            }
        } catch (InvocationTargetException | IllegalAccessException | TelegramApiException e) {
            throw new RuntimeException(e);
        }



    }
    private SendMediaGroup preparePhotoMessage(List<String> localPaths, ImagesOperation operation, String chatID) throws Exception {
        SendMediaGroup mediaGroup = new SendMediaGroup();
        ArrayList<InputMedia> media = new ArrayList<>();
        for (String path : localPaths) {
            InputMedia inputMedia = new InputMediaPhoto();
            processingImage(path, operation);
            inputMedia.setMedia(new java.io.File(path), path);
            media.add(inputMedia);
        }
        mediaGroup.setMedias(media);
        mediaGroup.setChatId(chatID);
        return mediaGroup;
    }
    private List<org.telegram.telegrambots.meta.api.objects.File> getFilesByMessage(Message message) {
        List<PhotoSize> photoSizes = message.getPhoto();
        if (photoSizes == null) return new ArrayList<>();
        ArrayList<org.telegram.telegrambots.meta.api.objects.File> files = new ArrayList<>();
        for (PhotoSize photoSize : photoSizes) {
            final String fileID = photoSize.getFileId();
            try {
                files.add(sendApiMethod(new GetFile(fileID)));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return files;
    }
    private ReplyKeyboardMarkup getKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        ArrayList<KeyboardRow> allKeyboardRows = new ArrayList<>();
        allKeyboardRows.addAll(getKeyboardRows(BotCommonCommands.class));
        allKeyboardRows.addAll(getKeyboardRows(FilterOperations.class));

        replyKeyboardMarkup.setKeyboard(allKeyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        return replyKeyboardMarkup;

    }

    private static ArrayList<KeyboardRow> getKeyboardRows(Class someClass) {
        Method[] methods = someClass.getDeclaredMethods();
        ArrayList<AppBotCommands> commands = new ArrayList<>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommands.class)) {
                commands.add(method.getAnnotation(AppBotCommands.class));
            }
        }
        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();
        int columnCount = 3;
        int rowCount = commands.size() / columnCount + ((commands.size() % columnCount == 0) ? 0 : 1);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            KeyboardRow row = new KeyboardRow();
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                int index = rowIndex * columnCount + columnIndex;
                if (index >= commands.size()) continue;
                AppBotCommands command = commands.get(rowIndex * columnCount + columnIndex);
                KeyboardButton keyboardButton = new KeyboardButton(command.name());
                row.add(keyboardButton);
            }
            keyboardRows.add(row);
        }
        return keyboardRows;
    }
    private String runCommand(String text) throws InvocationTargetException, IllegalAccessException {
        BotCommonCommands commands = new BotCommonCommands();
        Method[] methods = commands.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommands.class)) {
                AppBotCommands command = method.getAnnotation(AppBotCommands.class);
                if (command.name().equals(text)) {
                    method.setAccessible(true);
                    return (String) method.invoke(commands);
                }
            }
        }
        return null;
    }
    private SendMessage runCommonCommand(Message message) throws InvocationTargetException, IllegalAccessException {
        String text = message.getText();
        BotCommonCommands commands = new BotCommonCommands();
        Method[] methods = commands.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(AppBotCommands.class)) {
                AppBotCommands command = method.getAnnotation(AppBotCommands.class);
                if (command.name().equals(text)) {
                    method.setAccessible(true);
                    String responseText = (String) method.invoke(commands);
                    if (responseText != null) {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(message.getChatId().toString());
                        sendMessage.setText(responseText);
                        return sendMessage;
                    }
                }
            }
        }
        return null;
    }
    private Object runPhotoFilter (Message message) {
        final String text = message.getText();
        ImagesOperation operation = ImageUtils.getOperation(text);
        if (operation == null) return null;
        String chatID = message.getChatId().toString();
        Message photoMessage = messages.get(chatID);
        if (photoMessage != null) {
            List<org.telegram.telegrambots.meta.api.objects.File> files = getFilesByMessage(photoMessage);
            try {
                List<String> paths = PhotoMessageUtils.savePhotos(files, getBotToken());
                return preparePhotoMessage(paths, operation, chatID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatID);
            sendMessage.setText("this dialogue is starting to get pointless");
            return sendMessage;
        }
        return null;
    }
    private SendMessage runPhotoMessage(Message message) {
        List<org.telegram.telegrambots.meta.api.objects.File> files = getFilesByMessage(message);
        if (files.isEmpty()) {
            return null;
        }
        String chatID = message.getChatId().toString();
        messages.put(chatID, message);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        ArrayList<KeyboardRow> allKeyboardRows = new ArrayList<>(getKeyboardRows(FilterOperations.class));
        replyKeyboardMarkup.setKeyboard(allKeyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setChatId(chatID);
        sendMessage.setText("Choose the filter you want to use");
        return sendMessage;
    }




    @Override
    public String getBotUsername() {
        return "testbot_34543535_bot";

    }
    @Override
    public String getBotToken() {
        return "6690679335:AAH31yxMsdQPyGLPNMB5t-f7z3RNyqWNYXo";
    }


}
