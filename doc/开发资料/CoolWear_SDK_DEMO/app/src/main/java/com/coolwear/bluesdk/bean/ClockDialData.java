package com.coolwear.bluesdk.bean;

import com.coolwear.bluesdk.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Author: zhengyunbin
 * @CreateDate: 2021/3/24 0024 15:55
 */
public class ClockDialData {
    public static List<ClockDialTextColorBean> TEXT_COLORS = new ArrayList<>();
    public static List<ClockDialOtherData> TIME_POSITIONS = new ArrayList<>();
    public static List<ClockDialOtherData> ABOVE_TIMES = new ArrayList<>();
    public static List<ClockDialOtherData> BELOW_TIMES = new ArrayList<>();
    static {
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFFFFFFFF));
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFF00000A));
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFFF2DD36));
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFFE02F17));
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFFE88C8B));
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFF639AA9));
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFF051BF4));
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFFABE8E6));
        TEXT_COLORS.add(new ClockDialTextColorBean(0xFF87FA2A));

        TIME_POSITIONS.add(new ClockDialOtherData(0, R.string.watch_face_position_above));
        TIME_POSITIONS.add(new ClockDialOtherData(1, R.string.watch_face_position_below));

        ABOVE_TIMES.add(new ClockDialOtherData(0, R.string.watch_face_none, "",0));
        ABOVE_TIMES.add(new ClockDialOtherData(1, R.string.watch_face_date, "1/1",0));
        ABOVE_TIMES.add(new ClockDialOtherData(2, R.string.watch_face_sleep, "00h00m",R.mipmap.watch_sleep));
        ABOVE_TIMES.add(new ClockDialOtherData(4, R.string.watch_face_heart, "000",R.mipmap.watch_heartrate));
        ABOVE_TIMES.add(new ClockDialOtherData(8, R.string.watch_face_step, "0",R.mipmap.watch_steps));

        BELOW_TIMES.add(new ClockDialOtherData(0, R.string.watch_face_none, ""));
        BELOW_TIMES.add(new ClockDialOtherData(1, R.string.watch_face_date, "1/1"));
        BELOW_TIMES.add(new ClockDialOtherData(2, R.string.watch_face_sleep, "00h00m",R.mipmap.watch_sleep));
        BELOW_TIMES.add(new ClockDialOtherData(4, R.string.watch_face_heart, "000",R.mipmap.watch_heartrate));
        BELOW_TIMES.add(new ClockDialOtherData(8, R.string.watch_face_step, "0",R.mipmap.watch_steps));

    }
}