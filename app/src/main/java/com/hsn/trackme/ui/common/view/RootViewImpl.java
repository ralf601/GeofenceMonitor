package com.hsn.trackme.ui.common.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hsn.trackme.R;

/**
 * Created by hassanshakeel on 2/27/18.
 */

public class RootViewImpl implements ViewMvc {

    private View mRootView;

    public RootViewImpl(Context context, ViewGroup container) {
        mRootView = LayoutInflater.from(context).inflate(R.layout.mvc_view_frame_layout, container);
    }

    @Override
    public View getRootView() {
        return mRootView;
    }


    @Override
    public void initView(View view) {

    }

    @Override
    public void initListeners() {

    }
}

