

package com.coolwear.bluesdk.utils;

import android.app.Activity;
import android.app.Service;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

import ce.com.cenewbluesdk.entity.MyBleDevice;

/************************************************************
 * ,* Author:  zhengyunbin
 * ,* Description:  处理activity的安全退出
 * ,* Date: 2021/01/13
 ************************************************************/
public class ActivityManager {
    //保存所有Activity
    private static Stack<SoftReference<Activity>> activityStack;
    private static ActivityManager instance;
    private List<MyBleDevice> resultDeviceList = new ArrayList<>();//操作完成的设备
    //保存所有Service
    private LinkedList<Service> services = new LinkedList<Service>();

    private ActivityManager() {
        if (activityStack == null) {
            activityStack = new Stack();
        }
    }

    public static ActivityManager getAppManager() {
        if (instance == null) {
            instance = new ActivityManager();
        }
        return instance;
    }

    public void addActivity(Activity activity) {
        if (activityStack == null) {
            activityStack = new Stack();
        }
        activityStack.add(new SoftReference<Activity>(activity));
    }

    public boolean isHaveActivity(Class cls) {
        for (int i = 0, size = activityStack.size(); i < size; i++) {
            if (activityStack.get(i) != null && activityStack.get(i).get() != null) {
                if (activityStack.get(i).get().getClass().equals(cls)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Activity getCurrentActivity() {
        try {
            if (activityStack.lastElement() == null) return null;
            Activity activity = (Activity) activityStack.lastElement().get();
            return activity;
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            return null;
        }

    }

    public void finishActivity() {
        try {
            if (activityStack.lastElement() == null) return;
            Activity activity = (Activity) activityStack.lastElement().get();
            finishActivity(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void popActivity(Activity activity) {
        SoftReference<Activity> weakReference = null;
        if (activity != null) {
            for (SoftReference<Activity> activity1 : activityStack) {
                if (activity1 != null && activity1.get() != null) {
                    if (activity1.get().equals(activity)) {
                        weakReference = activity1;
                    }
                }
            }
            if (weakReference != null)
                activityStack.remove(weakReference);
            activity = null;
        }
    }

    /**
     * 注册Service以便集中“stopSelf()”
     *
     * @param service the service
     */
    public void addService(Service service) {
        services.add(service);
    }

    /**
     * Remove service.
     *
     * @param service the service
     */
    public void removeService(Service service) {
        services.remove(service);
    }

    /**
     * 所有的Service
     *
     * @return the services
     */
    public LinkedList<Service> getServices() {
        return services;
    }

    public void finishActivity(Activity activity) {
        if (activity != null) {
            popActivity(activity);
            activity.finish();
            activity = null;
        }
    }

    public void finishActivity(Class cls) {
        Stack<SoftReference<Activity>> activities = new Stack<>();
        for (int i = 0, size = activityStack.size(); i < size; i++) {
            if (activityStack.get(i) != null && activityStack.get(i).get() != null) {
                if (activityStack.get(i).get().getClass().equals(cls)) {
                    activityStack.get(i).get().finish();
                    activities.add(activityStack.get(i));
                }
            }
        }
        activityStack.removeAll(activities);
    }


    public void finishAllActivity() {
        for (int i = 0, size = activityStack.size(); i < size; i++) {
            if (null != activityStack.get(i) && null != activityStack.get(i).get()) {
                activityStack.get(i).get().finish();
            }
        }
        activityStack.clear();
    }

    public void finishActivityNoByCls(Class cls) {
        Stack<SoftReference<Activity>> activities = new Stack<>();
        for (int i = 0, size = activityStack.size(); i < size; i++) {
            if (null != activityStack.get(i) && null != activityStack.get(i).get() && !activityStack.get(i).get().getClass().equals(cls)) {
                activityStack.get(i).get().finish();
                activities.add(activityStack.get(i));
            }
        }
        activityStack.removeAll(activities);
    }

    public void AppExit() {
        clearActivitiesAndServices();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);//normal exit application
    }

    public void clearActivitiesAndServices() {
        finishAllActivity();
        for (Service service : services) {
            service.stopSelf();
        }
    }

    public static boolean isExist(String activityName) {
        for (SoftReference<Activity> activity : activityStack) {
            if (activity.getClass().getName().equals(activityName)) {
                return true;
            }
        }
        return false;
    }

    public List<MyBleDevice> getResultDeviceList() {
        return resultDeviceList;
    }

    public void setResultDeviceList(List<MyBleDevice> resultDeviceList) {
        this.resultDeviceList = resultDeviceList;
    }
}