package com.wzz.glide;

public interface MemoryCache {

    interface ResourceRemovedListener{
        void onResourceRemoved(Resource resource );
    }

    /**
     * 手动移除掉
     * @param key
     * @return
     */
    Resource remove2(Key key);
    Resource put(Key key , Resource resource );

    void setResourceRemovedListener(ResourceRemovedListener listener);

    void clearMemory();

    void trimMemory(int level);

}
