package com.wzz.glide;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ActiveResources {
    private ReferenceQueue<Resource> mReferenceQueue ;
    private Resource.ResourceListener mResourceListener ;

    private Thread queueThread;
    private volatile boolean isShutdown; // 判断线程是否停止

    /**
     * 使用弱引用 记录正在使用的资源
     * @param listener
     */
    private Map<Key , ResourceWeakReference> activeResources = new HashMap<>() ;

    public ActiveResources(Resource.ResourceListener listener){
        this.mResourceListener = listener ;
    }

    /**
     * 如果我们在创建一个引用对象时，指定了ReferenceQueue，那么当引用对象指向的对象达到合适的状态（根据引用类型不同而不同）时，
     * GC 会把引用对象本身添加到这个队列中，方便我们处理它，
     * 因为“引用对象指向的对象 GC 会自动清理，但是引用对象本身也是对象（是对象就占用一定资源），所以需要我们自己清理。”
     *
     * 链接：https://www.jianshu.com/p/f86d3a43eec5
     * @param key
     * @param resource
     */
    public void activate(Key key , Resource resource){
        resource.setResourceListener( key , mResourceListener );
        // 将resource添加到map中 当内存不足，resource被回收时，mReferenceQueue.remove()就会得到对应的ResourceWeakReference
        activeResources.put( key , new ResourceWeakReference(key, resource, getReferenceQueue() )) ;
    }

    public void deactivate(Key key){
        //相同的key 替换了新的value 返回旧的value
        ResourceWeakReference remove = activeResources.remove(key);
        if ( null != remove ){
            remove.clear();
        }
    }

    public Resource get(Key key){
        ResourceWeakReference weakReference = activeResources.get(key);
        if ( weakReference == null ){
            return null;
        }
        return weakReference.get() ;
    }


    /**
     * new Reference<Bitmap>( bitmap )
     *
     * 我们希望当一个对象被gc掉的时候通知用户线程，进行额外的处理时，就需要使用引用队列了。
     * ReferenceQueue即这样的一个对象，当一个obj被gc掉之后，其相应的包装类，即ref对象会被放入queue中。
     * 我们可以从queue中获取到相应的对象信息，同时进行额外的处理。比如反向操作，数据清理等。
     *
     * ★★★★  这里通过监控value变化，反向修改map，以达到控制kv的目的，避免出现无用的kv映射。 ★★★
     *
     * 创建引用队列
     * @return
     */
    private ReferenceQueue<Resource> getReferenceQueue(){
        if ( mReferenceQueue == null ){
            mReferenceQueue = new ReferenceQueue<>();
            queueThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while ( !isShutdown ){
                        try {

                            // 当内存不足，弱引用中的对象即resource被回收  mReferenceQueue.remove()就会得到对应的ResourceWeakReference， 并从map中移除此键值对
                            ResourceWeakReference ref = (ResourceWeakReference) mReferenceQueue.remove();

                            // 其实就是拿到反向引用的key值(这里的value已经不存在了)，因为kv映射已没有意义，将其从map中移除掉。
                            activeResources.remove( ref.mKey );

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "glide-active-resource");

            queueThread.start();
        }

        return mReferenceQueue ;
    }

    /**
     * 停止线程
     */
    private void shutdown(){
        isShutdown = true ;
        if ( queueThread == null ){
            return;
        }
        queueThread.interrupt();
        try {
            queueThread.join(TimeUnit.SECONDS.toMillis(5));
            if ( queueThread.isAlive() ){
                throw new RuntimeException("Failed to join in time");
            }
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }


    /**
     * 其内部提供2个构造函数，一个带queue，一个不带queue。
     * 其中queue的意义在于，我们可以在外部对这个queue进行监控。
     * 即如果有对象即将被回收，那么相应的reference对象就会被放到这个queue里。我们拿到reference，就可以再作一些事务。
     */
    static class ResourceWeakReference extends WeakReference<Resource>{

        private Key mKey;
        public ResourceWeakReference(Key key,Resource referent, ReferenceQueue<? super Resource> q) {
            super(referent, q);
            this.mKey = key ;
        }
    }


}
