package com.winlator.cmod.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;

import com.winlator.cmod.renderer.VulkanRenderer;
import com.winlator.cmod.xserver.XServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("ViewConstructor")
public class XServerView extends SurfaceView implements SurfaceHolder.Callback {
    private final VulkanRenderer renderer;
    private final ExecutorService eventExecutor = Executors.newSingleThreadExecutor();

    public XServerView(Context context, XServer xServer) {
        super(context);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        getHolder().addCallback(this);
        renderer = new VulkanRenderer(this, xServer);
    }

    public VulkanRenderer getRenderer() {
        return renderer;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        renderer.onSurfaceCreated(holder.getSurface());
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        renderer.onSurfaceChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        renderer.onSurfaceDestroyed();
    }

    public void queueEvent(Runnable r) {
        eventExecutor.execute(r);
    }

    public void onPause() {}
    public void onResume() {}
}
