package com.coolwear.bluesdk.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.coolwear.bluesdk.R;

/**
 * Created by Administrator on 2017/1/11.
 */

public class MySwitchButton extends View {
    private int mRadius;
    private int mStroke;
    private Paint mPaint;
    private boolean open = false;
    private boolean isTouching = false;
    private float mWidth;
    private float mHeight;
    private float mfPos;
    private RectF oval1,oval2;

    private OnSwithButonChangedListener mOnSwithButonChangedListener = new OnSwithButonChangedListener() {
        @Override
        public void onCheckedChanged(boolean isOpen) {

        }
    };




    public MySwitchButton(Context context) {
        this(context,null);
    }

    public MySwitchButton(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MySwitchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        invalidate();
    }

    public void init(){
        mPaint = new Paint();
        //   mPaint.setColor(Color.GRAY);
        //   mPaint.setStrokeWidth(2f);
        oval1 = new RectF();// 设置个新的长方形，左半圈
        oval2 = new RectF();// 设置个新的长方形，右半圈


    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        if (!open) { //关
            //画背景
            mPaint.setStrokeWidth(3f);
            mPaint.setStyle(Paint.Style.STROKE);//充满
            mPaint.setColor(getResources().getColor(R.color.switchGray));
            mPaint.setDither(true);
            mPaint.setAntiAlias(true);// 设置画笔的锯齿效果
         //   RectF oval3 = new RectF(0, 0, getWidth()-5, getHeight()-5);// 设置个新的长方形
            //    canvas.drawRoundRect(oval3, (getWidth()-10) / 2, (getWidth()-10) / 2, mPaint);//第二个参数是x半径，第三个参数是y半径

         //   RectF oval1 = new RectF(2, 2, getHeight()-2, getHeight()-2);// 设置个新的长方形
         //   RectF oval2 = new RectF(getWidth()-getHeight()+2, 2, getWidth()-2, getHeight()-2);// 设置个新的长方形
            oval1.set(12, 12, getHeight()-12, getHeight()-12);  //对象初始化在onDraw外面
            oval2.set(getWidth()-getHeight()+12, 12, getWidth()-12, getHeight()-12);

            canvas.drawArc(oval1,90,180,false,mPaint);
            canvas.drawArc(oval2,270,180,false,mPaint);
            canvas.drawLine(getHeight()/2,12,getWidth()-getHeight()/2,12,mPaint);
            canvas.drawLine(getHeight()/2,getHeight()-12,getWidth()-getHeight()/2,getHeight()-12,mPaint);

            //   canvas.drawCircle(getHeight()/2,getHeight()/2,getHeight()/2,mPaint);
            //  canvas.drawCircle(getWidth()-getHeight()/2,getHeight()/2,getHeight()/2,mPaint);


            //   canvas.drawRect(getHeight()/2,0,getWidth()-getHeight()/2,getHeight(),mPaint);

            if (isTouching){
                if (mWidth<getHeight() / 2 ){
                    mWidth = getHeight()/2;
                }
                if (mWidth > getWidth()-getHeight()/2){
                    mWidth = getWidth()-getHeight()/2;
                }
                mPaint.setColor(getResources().getColor(R.color.white));
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);//充满
                canvas.drawCircle(mWidth , getHeight() / 2, ((getHeight()-10)/2) -1, mPaint);
            }else {
                Paint paint = new Paint();
                paint.setStrokeWidth(3f);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);//充满
                paint.setColor(getResources().getColor(R.color.switchGray));
                paint.setDither(true);
                paint.setAntiAlias(true);// 设置画笔的锯齿效果
                canvas.drawCircle(getHeight() / 2 + 6, getHeight() / 2, getHeight() / 2 - 5 , paint);
//                canvas.drawCircle(getWidth() - getHeight() / 2, getHeight() / 2, getHeight() / 2 - 5, paint);

            }

        }else {  //开
            //画背景
           /* mPaint.setStrokeWidth(3f);
            mPaint.setStyle(Paint.Style.FILL);//充满
            mPaint.setColor(Color.GREEN);
            mPaint.setAntiAlias(true);// 设置画笔的锯齿效果
            RectF oval3 = new RectF(0, 0, getWidth(), getHeight());// 设置个新的长方形
            //     canvas.drawRoundRect(oval3, getWidth() / 2, getWidth() / 2, mPaint);//第二个参数是x半径，第三个参数是y半径
            oval1.set(2, 2, getHeight()-2, getHeight()-2);  //对象初始化在onDraw外面
            oval2.set(getWidth()-getHeight()+2, 2, getWidth()-2, getHeight()-2);
            canvas.drawArc(oval1,90,180,false,mPaint);
            canvas.drawArc(oval2,270,180,false,mPaint);

        //    canvas.drawCircle(getHeight()/2,getHeight()/2,getHeight()/2,mPaint);
          //  canvas.drawCircle(getWidth()-getHeight()/2,getHeight()/2,getHeight()/2,mPaint);
            canvas.drawRect(getHeight()/2,0,getWidth()-getHeight()/2,getHeight(),mPaint);*/

            mPaint.setStrokeWidth(getHeight()-24);
            mPaint.setStyle(Paint.Style.FILL);//充满
            mPaint.setColor(getResources().getColor(R.color.green_93));
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setAntiAlias(true);

            canvas.drawLine(getHeight()/2,getHeight()/2,getWidth()-getHeight()/2,getHeight()/2,mPaint);


            if (isTouching){
                if (mWidth<getHeight() / 2 ){
                    mWidth = getHeight()/2;
                }
                if (mWidth>getWidth()-getHeight()/2){
                    mWidth=getWidth()-getHeight()/2;
                }
                mPaint.reset();
                mPaint.setColor(getResources().getColor(R.color.green_50));
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);//充满

                mPaint.setAntiAlias(true);// 设置画笔的锯齿效果
               // canvas.drawCircle(mWidth, getHeight() / 2, getHeight() / 2 - 8, mPaint);
                canvas.drawCircle(mWidth , getHeight() / 2, ((getHeight()-10)/2) -1, mPaint);

                mPaint.reset();
                mPaint.setColor(getResources().getColor(R.color.green_50));
                mPaint.setStrokeWidth(3f);
                mPaint.setStyle(Paint.Style.STROKE);//充满
               // canvas.drawCircle(mWidth, getHeight() / 2, getHeight() / 2 - 8, mPaint);
                canvas.drawCircle(mWidth , getHeight() / 2, ((getHeight()-10)/2) -1, mPaint);

                //   canvas.drawCircle(mWidth , getHeight() / 2, ((getHeight()-10)/2) -1, mPaint);
            }else {
                mPaint.reset();
                mPaint.setColor(Color.WHITE);
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);//充满

                mPaint.setAntiAlias(true);// 设置画笔的锯齿效果
                canvas.drawCircle(getWidth() - getHeight() / 2, getHeight() / 2, getHeight() / 2 - 5, mPaint);

                mPaint.reset();
                mPaint.setColor(getResources().getColor(R.color.green_50));
                mPaint.setStrokeWidth(3f);
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);//充满

                canvas.drawCircle(getWidth() - getHeight() / 2, getHeight() / 2, getHeight() / 2 - 5, mPaint);

            }
        }


    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("rd9511", "ACTION_DOWN");
                isTouching = true;
                mWidth = event.getX();
                mHeight = event.getY();
                mfPos = mWidth;
                if(this.getParent().getParent()!=null){
                    Log.d("rd95",this.getParent().getParent().toString());
                    this.getParent().getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("rd9511", "ACTION_MOVE");
                isTouching =true;
                mWidth = event.getX();
                mHeight = event.getY();
                float b =mWidth > mfPos ? mWidth - mfPos : mfPos - mWidth;
                if (b > 5){  //当做滑动事件
                    if (mWidth > getWidth()/2){
                        open = true;
                    }else {
                        open = false;
                    }
                }

                invalidate();

                break;
            case MotionEvent.ACTION_UP:
                Log.d("rd9511", "ACTION_UP");

                isTouching = false;
                mWidth = event.getX();
                mHeight = event.getY();
                float a =mWidth > mfPos ? mWidth - mfPos : mfPos - mWidth;
                if (a < 5){   //当做点击事件
                    open = !open;
                }

                invalidate();
                if (mOnSwithButonChangedListener != null) {
                    mOnSwithButonChangedListener.onCheckedChanged(isOpen());
                }
                break;

        }
        return true;
    }


    public  interface OnSwithButonChangedListener{
        void onCheckedChanged(boolean isOPen);
    }

    public void setmOnSwithButonChangedListener(OnSwithButonChangedListener mOnSwithButonChangedListener) {
        this.mOnSwithButonChangedListener = mOnSwithButonChangedListener;
    }
}
