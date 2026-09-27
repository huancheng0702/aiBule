package com.coolwear.bluesdk.view;

import androidx.annotation.Nullable;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

public abstract class CommonAdapter<T> extends BaseQuickAdapter<T, BaseViewHolder> {
    public CommonAdapter(int layoutResId, @Nullable List<T> data) {
        super(layoutResId, data);
    }
    public CommonAdapter(int layoutResId) {
        this(layoutResId, new ArrayList<>());
    }
    @Override
    protected abstract void convert(BaseViewHolder holder, T item);
    public int getLastPosition(){
        if(getData()!=null){
            if(getData().size()>0){
                return getData().size()-1;
            }
        }
        return 0;
    }
}
