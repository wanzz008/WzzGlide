package com.wzz.glide;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;


/**
 *
 * 在Android中官网推荐使用LruCache作为内存缓存，
 * LruCache实际上就是一个LinkedHashMap( 补充知识：LinkedHashMap是一个双向循环列表，不支持线程安全，LruCache对它进行了封装添加了线程安全操作)，
 * 里面保存了一定数量的对象强引用，每次添加的新对象都是在链表的头，当分配的空间用完的时候会把末尾的对象移除，移除的对象就可以被gc回收了。
 * 这里需要注意一下LruCache的容量，这个容量既不能太大，会造成OOM，又不能太小，起不到缓存的作用。
 *
 */
/**
 * LruCache封装了LinkedHashMap，提供了LRU(Least Recently Used 最近最少使用算法)缓存的功能；
 *
 * LruCache通过trimToSize方法自动删除最近最少访问的键值对；
 *
 * LruCache不允许空键值， LinkedHashMap允许；
 *
 * LruCache线程安全， LinkedHashMap线程不安全；
 *
 * 继承LruCache时，必须要复写sizeOf方法，用于计算每个条目的大小。在put和get的时候会调用safeSizeOf(K key, V value)，safeSizeOf(K key, V value)会调用 sizeOf (K key, V value)，这个方法默认返回1。
 *
 */

/**
 * Created by dugaolong on 17/1/3.
 * 作为缓存，肯定有一个缓存的大小，这个大小是可以设定的（自定义sizeOf()）。
 * 当你访问了一个item（需要缓存的对象），这个item应该被加入到内存中，然后移动到一个队列的顶部，
 * 如此循环后这个队列的顶部应该是最近访问的item了，
 * 而队尾部就是很久没有访问的item，这样我们就应该对队尾部的item优先进行回收操作。
 */

public class LruResourceCache extends LruCache<Key, Resource> implements MemoryCache{

    private int max = 10*1024*1024;//最大缓存大小10M,如果缓存超过10M,会自动回收。

    final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);//获取应用在系统中的最大内存分配
    //分配1/8的应用内存作为缓存空间
    final int cacheSize = maxMemory / 8;

    public LruResourceCache(int maxSize) {
        super(maxSize);
    }


    /**
     *  sizeOf返回的是你缓存的每条item的大小，每次添加图片会被调用
     * @param key
     * @param resource
     * @return 返回图片的占用字节数而不是图片的个数
     */
    @Override
    protected int sizeOf(@NonNull Key key, @NonNull Resource resource) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            return resource.getBitmap().getAllocationByteCount() ;
        }
        return resource.getBitmap().getByteCount();
    }

    @Override
    protected void entryRemoved(boolean evicted, @NonNull Key key, @NonNull Resource oldValue, @Nullable Resource newValue) {

        if ( mRemovedListener!= null && oldValue != null && newValue!= null && !isRemoved){ // remove的不回调
            mRemovedListener.onResourceRemoved( oldValue );
        }

    }

    boolean isRemoved ;

    @Override
    public Resource remove2(Key key) {
        //我们主动remove的不回调
        isRemoved = true ;
        Resource remove = remove(key);
        isRemoved = false ;

        return remove;
    }


    private ResourceRemovedListener mRemovedListener ;

    @Override
    public void setResourceRemovedListener(ResourceRemovedListener listener) {
        this.mRemovedListener = listener ;
    }

    /**
     * 全部清除
     */
    @Override
    public void clearMemory() {
        evictAll();
    }

    @Override
    public void trimMemory(int level) {
        if ( level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND){
            clearMemory();
        }else if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN){
            trimToSize( maxSize() / 2 );
        }
    }

}
