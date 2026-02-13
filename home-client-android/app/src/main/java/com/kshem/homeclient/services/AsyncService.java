package com.kshem.homeclient.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncService {
    private static volatile AsyncService instance;
    private final ExecutorService pool;

    private AsyncService(){
        this.pool = Executors.newFixedThreadPool(4);
    }

    public static void execute(Runnable runnable){
        if(instance == null){
            synchronized (AsyncService.class){
                if (instance == null){
                    instance = new AsyncService();
                }
            }
        }
        try {
            instance.pool.execute(runnable);
        }catch (Exception exception){
            exception.printStackTrace();
        }

    }
}
