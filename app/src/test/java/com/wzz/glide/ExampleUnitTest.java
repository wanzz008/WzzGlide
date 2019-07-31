package com.wzz.glide;

import org.junit.Test;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    private int _1M = 1*1024*1024;//最大缓存大小10M,如果缓存超过10M,会自动回收。
    private ReferenceQueue<byte[]> referenceQueue = new ReferenceQueue();

    @Test
    public void useAppContext() {



        new Thread(new Runnable() {
            @Override
            public void run() {
                WeakReference<byte[]> weakReference ;
                int cnt = 0 ;
                while (true){
                    try {
                        weakReference = (WeakReference<byte[]>) referenceQueue.remove();

                        System.out.println( ( cnt++ ) + "回收了:" + weakReference + "  " + weakReference.get());

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();


        Object value = new Object();
        Map<Object, Object> map = new HashMap<>();
        for(int i = 0;i < 1000;i++) {
            byte[] bytes = new byte[_1M];
            WeakReference<byte[]> weakReference = new WeakReference<>(bytes, referenceQueue);
            map.put(weakReference, value);
//            map.put(bytes, value);
        }
        System.out.println("map.size->" + map.size());
    }

}