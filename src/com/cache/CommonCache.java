package com.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//Предполагается что отдельный экземпляр кэша будет хранить элементы одного типа, указанного при создании экземпляра
public class CommonCache<T>{
    private long timeToLive;
    private long checkTime;
    private Timer timer;
    //применена коллекция предназначенная для параллельных операций, поэтому дополнительная синхронизация не требуется
    private ConcurrentHashMap<UUID, ElementObject> elementsCashe;

    CommonCache(int timeToLive, @NotNull TimeUnit timeUnit, long checkTimeMillis){
        elementsCashe = new ConcurrentHashMap<>();
        setTimeToLive(timeToLive, timeUnit);
        checkTime = checkTimeMillis;
        timer = new Timer(true);//потоковый демон
        //первый запуск произойдет через время checkTime и будет повторяться с тем же интервалом
        timer.schedule(new CheckTime(), checkTime, checkTime);
    }

    public boolean setTimeToLive(long timeToLive, @NotNull TimeUnit timeUnit) {
        switch (timeUnit.name()){
            case "MILLISECONDS": this.timeToLive = timeToLive;
                return true;
            case "SECONDS": this.timeToLive = TimeUnit.SECONDS.toMillis(timeToLive);
                return true;
            case "MINUTES": this.timeToLive = TimeUnit.MINUTES.toMillis(timeToLive);
                return true;
            case "HOURS": this.timeToLive = TimeUnit.HOURS.toMillis(timeToLive);
                return true;
            default: return false;
        }
    }

    //возвращает null если элемент не найден
    public T getElement(UUID uuid){
        var elementObject = GetElementObject(uuid);
        return elementObject != null ? elementObject.object : null;
    }

    private ElementObject GetElementObject(UUID uuid){
        return elementsCashe.getOrDefault(uuid,null);
    }

    //предполагается что элементы null допустимы
    public UUID setElement(T element){
        var elementObject = new ElementObject(element);
        var key = UUID.randomUUID();
        //если такой ключ уже есть то генерируем новый
        while (elementsCashe.containsKey(key))
            key = UUID.randomUUID();
        elementsCashe.put(key, elementObject);
        return key;
    }

    public boolean UpdateElementAddTime(UUID uuid){
        var elementObject = GetElementObject(uuid);
        if(elementObject!=null){
            elementObject.time=System.currentTimeMillis();
            elementsCashe.put(uuid, elementObject);
            return true;
        }
        else
            return false;
    }

    public void ClearCache()
    {
        elementsCashe.clear();
    }

    private class ElementObject{
        T object;
        long time;

        ElementObject(T object){
            this.object = object;
            this.time = System.currentTimeMillis();
        }
    }

    private class CheckTime extends java.util.TimerTask{
        @Override
        public void run() {
            for (Map.Entry<UUID, ElementObject> element : elementsCashe.entrySet()) {
                if(System.currentTimeMillis() - element.getValue().time>timeToLive)
                    elementsCashe.remove(element.getKey());
            }
        }
    }
}
