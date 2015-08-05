package com.dalinoo.courier;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Henry on 15/7/6.
 */
public class Order {

    public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";
    public static final int PAY_TYPE_ONLINE = 1;
    public static final int PAY_TYPE_OFFLINE = 2;

    public String orderID;
    public Date   orderTime;
    public Date   expressTime;
    public String userName;
    public String mobileNo;
    public double payment;
    public int    payType;
    public String location;
    public String address;
    public int    status;
    public String timeString;

    //设定配送时间字符串（配送时间为当前时间开始计算1小时内）
    void setTimeString() {

        //读取起始时间：
        DateFormat dateFormat = new SimpleDateFormat( TIME_FORMAT );

        //计算结束时间：(考虑到不会出现24点以后的数据场景，直接对当前时间+1）
        DateFormat hourDate = new SimpleDateFormat( "HH");
        int hours = Integer.parseInt( hourDate.format(expressTime) );
        ++hours;

        //填充时间字符串：（处理10点以内的一位数数据需要补充0）
        if( hours < 10 ) timeString = dateFormat.format( expressTime ) + " - 0" + hours + ":00";
        else timeString = dateFormat.format( expressTime ) + " - " + hours + ":00";
    }

}
