package com.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

//Предполагается что отдельный экземпляр кэша будет хранить элементы одного типа, указанного при создании экземпляра
public class CommonCache<T>{
    private long timeToLive;
    private long checkTime;
    private Timer timer;
    private Semaphore semaphore;
    //применена коллекция предназначенная для параллельных операций, поэтому дополнительная синхронизация не требуется
    //элементы сортируются по ключу, поэтому появляется удобная возможность для удаления устаревших ключей
    private ConcurrentSkipListMap<Long, T> elementsCashe;


    CommonCache(int timeToLive, @NotNull TimeUnit timeUnit, long checkTimeMillis){
        elementsCashe = new ConcurrentSkipListMap<>();
        setTimeToLive(timeToLive, timeUnit);
        checkTime = checkTimeMillis;
        timer = new Timer(true);//потоковый демон
        semaphore = new Semaphore(1);
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

    public T getElement(Long key){
        return elementsCashe.getOrDefault(key,null);
    }

    //предполагается что элементы null допустимы
    public  Long setElement(T element) throws InterruptedException {
        //гарантируем что метод будет использоваться только из одного внешнего потока, благодаря чему Thread.sleep(1)
        //будет работать корректно
        semaphore.acquire();
        var key = System.currentTimeMillis();
        elementsCashe.put(key, element);
        //гарантируем отсутствие дублирования ключей, благодаря интервалу между вставками минимум 1 миллисекунда
        Thread.sleep(1);
        semaphore.release();
        return key;
    }
    //метод после обновления времени возвращает новый ключ, если ключ не найден возвращает null
    public Long UpdateElementAddTime(Long key) throws InterruptedException {
        var element = getElement(key);
        if (element != null) {
            return  setElement(element);
        }
        return null;

    }

    public void ClearCache()
    {
        elementsCashe.clear();
    }

    private class CheckTime extends java.util.TimerTask{
        @Override
        public void run() {
            elementsCashe.headMap(System.currentTimeMillis() - timeToLive).clear();
        }
    }
}
