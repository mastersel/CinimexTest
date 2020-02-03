package com.cache;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        //Предполагается что класс CommonCache при добавлении элемента возвращает пользователю ключ являющийся таймкодом
        //добавления элемента
        //пользователь класса CommonCache организует хранение ключей и запрос данных с их помощью

        //примитивная имитация работы с кэшем:
        //коллекция для хранения ключей
        var list = new ArrayList<Long>();
        //устанавливаем тип элементов для данного экземпляра кэша - String.
        // время жизни элемента в кэше = 15 секунд, через 5 секунд начнется чистка кэша с интервалом в 5 секунд
        var commonCache = new CommonCache<String>(15, TimeUnit.SECONDS, 5000);
        for(var i = 0; i<80;i++){
             //после добавления элемента в кэш, возвращается его UUID
             list.add(commonCache.setElement("Строка "+i));
             Thread.sleep(1000);
             System.out.println(new Date());
             //через каждую секунду демонстрируем содержимое кэша
             for (var key:list){
                 var element = commonCache.getElement(key);
                 if(element!=null)
                     System.out.println(element);
             }
             //на 25й секунде сокращаем время кэширования до 10 секунд
             if(i==25) {
                 commonCache.setTimeToLive(10, TimeUnit.SECONDS);
                 System.out.println("Сократили время жизни до 10 секунд");
             }
             //на 35й секунде одному из элементов обновляем время добавления на текущее
             if(i==35) {
                 var key =list.get(list.size() - 8);
                 var updatedKey = commonCache.UpdateElementAddTime(key);
                 if(updatedKey!=null) {
                     list.set(list.size() - 8, updatedKey);
                     System.out.println("Элемент \"" + commonCache.getElement(updatedKey) + "\" обновлен в " + new Date());
                 }
             }
             //на 50й секунде очищаем кэш
             if(i==50) {
                 commonCache.ClearCache();
                 System.out.println("Кэш очищен");
             }
        }
    }
}
