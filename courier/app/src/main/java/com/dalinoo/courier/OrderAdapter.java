package com.dalinoo.courier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Henry on 15/7/6.
 */
public class OrderAdapter extends ArrayAdapter<Order> {

    private LayoutInflater mInflater;
    private int mResouce;
    public OrderActivity orderActivity;

    public OrderAdapter(Context context, int resource, List<Order> objects) {
        super(context, resource, objects);

        mInflater = LayoutInflater.from( context );
        mResouce = resource;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        LinearLayout view = null;

        //检查并创建列表项：
        if( convertView == null) {
            view = (LinearLayout)mInflater.inflate(  mResouce, null );

        }else {
            view = (LinearLayout)convertView;
        }

        //获取当前列表项对应订单数据：
        final Order order = getItem( position );

        //获取对应列表项对应控件：
        TextView txtExpressTimeTitle = (TextView)view.findViewById( R.id.txtExpressTimeTitle );
        TextView txtExpressTime = (TextView)view.findViewById( R.id.txtExpressTime );
        TextView txtAddress = (TextView)view.findViewById( R.id.txtAddresss );
        TextView txtContact = (TextView)view.findViewById( R.id.txtContact );
        TextView txtPayment = (TextView)view.findViewById( R.id.txtPayment );
        LinearLayout layoutControl = (LinearLayout)view.findViewById( R.id.layoutControl );
        Button   btnAction = (Button)view.findViewById( R.id.btnAction );
        Button   btnModify = (Button)view.findViewById( R.id.btnModify );
        Button   btnCancel = (Button)view.findViewById( R.id.btnCancel );
        Button   btnPhoneCall = (Button)view.findViewById( R.id.btnPhoneCall );

        //对配送时间进行检查：
        Date now = new Date();
        long timeSpan = order.expressTime.getTime() - now.getTime();
        int nColor = 0;

        //超时：
        if( timeSpan < 0 ) nColor = orderActivity.getResources().getColor(R.color.express_overtime_background);
        else {
            long days = timeSpan / (1000 * 60 * 60 * 24);
            long hours = (timeSpan-days*(1000 * 60 * 60 * 24))/(1000* 60 * 60);
            //在一小时内：
            if( days == 0 && hours == 0 ) nColor = orderActivity.getResources().getColor(R.color.express_intime_background);

            //还没到时间:
            else nColor = orderActivity.getResources().getColor(R.color.express_normal_background);
        }

        //设置时间框背景颜色：
        txtExpressTimeTitle.setBackgroundColor( nColor );
        txtExpressTime.setBackgroundColor( nColor );

        //为控件设定显示内容：
        txtExpressTime.setText( order.timeString );


        txtAddress.setText( order.location + " " + order.address );
        txtContact.setText( order.userName );
        String strPayment = String.valueOf( order.payment ) + orderActivity.getString(R.string.pay_unit);
        if( order.payType == Order.PAY_TYPE_ONLINE ){
            strPayment +=  orderActivity.getString(R.string.pay_type_online);
            txtPayment.setTextColor( orderActivity.getResources().getColor(R.color.pay_type_online));
        }
        else if( order.payType == Order.PAY_TYPE_OFFLINE ){
            strPayment += orderActivity.getString(R.string.pay_type_offline);
            txtPayment.setTextColor( orderActivity.getResources().getColor(R.color.pay_type_offline));
        }
        txtPayment.setText( strPayment );

        switch ( order.status ){
            case OrderActivity.ORDER_TYPE_NEW:
                btnAction.setText( orderActivity.getString(R.string.btn_action_new) );
                break;
            case OrderActivity.ORDER_TYPE_ACCEPT:
                btnAction.setText( orderActivity.getString(R.string.btn_action_accept) );
                break;
            case OrderActivity.ORDER_TYPE_EXPRESS:
                btnAction.setText( orderActivity.getString(R.string.btn_action_express) );
                break;
            default:
        }

        if( position == orderActivity.mCurrentPostion ) {
            layoutControl.setVisibility( View.VISIBLE );
        }else{
            layoutControl.setVisibility( View.GONE );
        }

        //开始/完成配送的按键事件:
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderActivity.updateOrder(order.orderID, order.status + 1);
            }
        });

        //拨叫用户联系电话的按键事件:
        btnPhoneCall.setOnClickListener( new View.OnClickListener() {
            @Override
            public  void onClick(View v ) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                Uri data = Uri.parse("tel:" + order.mobileNo);
                intent.setData(data);
                orderActivity.startActivity(intent);
            }
        });

        //取消当前订单事件:
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder( orderActivity ).setTitle("确认取消此订单吗？")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //发起取消订单 HTTP POST:
                                orderActivity.updateOrder(order.orderID, OrderActivity.ORDER_TYPE_CANCEL );
                            }
                        })
                        .setNegativeButton("返回", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 点击“返回”后的操作,这里不设置没有任何操作
                            }
                        }).show();
            }
        });

        return view;
    }
}
