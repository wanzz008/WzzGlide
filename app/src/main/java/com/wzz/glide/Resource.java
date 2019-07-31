package com.wzz.glide;

import android.graphics.Bitmap;

public class Resource {

    private Bitmap bitmap;

    private int acquired ;

    private Key key ;

    private ResourceListener listener;

    public Resource(Bitmap bitmap) {
        this.bitmap = bitmap;
    }


    public Bitmap getBitmap() {
        return bitmap;
    }

    public interface ResourceListener{
        void onResourceReleased(Key key);
    }


    public void setResourceListener(Key key ,ResourceListener listener){
        this.key = key ;
        this.listener = listener ;
    }

    public void acquire(){
        if ( bitmap.isRecycled() ){
            throw new IllegalStateException("Cannot acquire a recycled resource");
        }

        ++ acquired ;
    }

    public void release(){
        if ( --acquired == 0 ){
            listener.onResourceReleased( key );
        }
    }

    public void recycle(){
        if ( acquired > 0 ){
            return;
        }
        if ( !bitmap.isRecycled() ){
            bitmap.recycle();
        }
    }


}
