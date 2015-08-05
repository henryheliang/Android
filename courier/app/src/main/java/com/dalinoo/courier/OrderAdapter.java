package com.dalinoo.courier;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    private void SetExpressTime( LinearLayout view, Order order ) {

        TextView txtExpressTimeTitle = (TextView)view.findViewById( R.id.txtExpressTimeTitle );
        TextView txtExpressTime = (TextView)view.findViewById( R.id.txtExpressTime );

        int nColor = 0;

        //对配送时间进行检查(未结算页面无须检查）：
        if( order.status != OrderActivity.ORDER_TYPE_BALANCE ) {

            Date now = new Date();
            long oneHour = 60*60*1000;
            long oneDay = oneHour*24;
            long timeSpan = order.expressTime.getTime() + oneHour - now.getTime();

            //超时：
            if( timeSpan < 0 )
                nColor = orderActivity.getResources().getColor(R.color.express_overtime_background);
            else {
                long days = timeSpan / oneDay;
                long hours = (timeSpan-days*oneDay)/oneHour;
                //在一小时内：
                if( days == 0 && hours == 0 ) nColor = orderActivity.getResources().getColor(R.color.express_intime_background);

                    //还没到时间:
                else nColor = orderActivity.getResources().getColor(R.color.express_normal_background);
            }
        } else {
            nColor = orderActivity.getResources().getColor( R.color.title_balance );
        }


        //设置时间框背景颜色：
        txtExpressTimeTitle.setBackgroundColor(nColor);
        txtExpressTime.setBackgroundColor( nColor );

        //为控件设定显示内容：
        txtExpressTime.setText( order.timeString );
    }

    private void SetPayment( LinearLayout view, Order order ) {

        TextView txtPayment = (TextView)view.findViewById( R.id.txtPayment );
        //订单金额信息：
        String strPayment = String.valueOf( order.payment ) + orderActivity.getString(R.string.pay_unit);
        //在线支付类文字颜色：
        if( order.payType == Order.PAY_TYPE_ONLINE ){
            strPayment +=  orderActivity.getString(R.string.pay_type_online);
            txtPayment.setTextColor( orderActivity.getResources().getColor(R.color.pay_type_online));
        }
        //货到付款类文字颜色：
        else if( order.payType == Order.PAY_TYPE_OFFLINE ){
            strPayment += orderActivity.getString(R.string.pay_type_offline);
            txtPayment.setTextColor( orderActivity.getResources().getColor(R.color.pay_type_offline));
        }
        txtPayment.setText( strPayment );
    }

    private void SetAction( LinearLayout view, final Order order ) {

        Button   btnAction = (Button)view.findViewById( R.id.btnAction );

        //根据不同的订单页面动作按钮显示不同的文字信息：
        switch ( order.status ){
            case OrderActivity.ORDER_TYPE_NEW: //接受订单：
                btnAction.setText( orderActivity.getString(R.string.btn_action_new) );
                break;
            case OrderActivity.ORDER_TYPE_ACCEPT: //开始配送：
                btnAction.setText( orderActivity.getString(R.string.btn_action_accept) );
                break;
            case OrderActivity.ORDER_TYPE_EXPRESS: //配送完成：
                btnAction.setText( orderActivity.getString(R.string.btn_action_express) );
                break;
            default:
        }

        //开始/完成配送的按键事件:
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                orderActivity.updateOrder(order.orderID, order.status + 1);
            }
        });
    }

    private void SetCancel( LinearLayout view, final Order order ) {

        Button   btnCancel = (Button)view.findViewById( R.id.btnCancel );

        //取消当前订单事件:
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder( orderActivity ).setTitle(orderActivity.getString(R.string.content_cancel_confirm))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(orderActivity.getString(R.string.content_confirm),
                                new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //发起取消订单 HTTP POST:
                                orderActivity.updateOrder(order.orderID, OrderActivity.ORDER_TYPE_CANCEL );
                            }
                        })
                        .setNegativeButton(orderActivity.getString(R.string.content_return),
                                new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // 点击“返回”后的操作,这里不设置没有任何操作
                            }
                        }).show();
            }
        });
    }

    private void SetPhoneCall( LinearLayout view, final Order order ) {

        Button   btnPhoneCall = (Button)view.findViewById( R.id.btnPhoneCall );

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
    }

    private void SetSelected( int position, LinearLayout view, Order order ) {

        LinearLayout orderExtendLayout = (LinearLayout)view.findViewById( R.id.orderExtendLayout );
        LinearLayout orderCommandLayout = (LinearLayout)view.findViewById(R.id.orderCommandLayout );
        LinearLayout orderDetailLayout = (LinearLayout)view.findViewById( R.id.orderDetailLayout );

        //选中项展示详情和命令列表：
        if( position == orderActivity.mCurrentPostion
                && orderExtendLayout.getVisibility() == View.GONE ) {

            //显示扩展信息区：
            orderExtendLayout.setVisibility(View.VISIBLE);

            //清除原有控件:
            orderDetailLayout.removeAllViews();

            //设定订单详情文本框格式：
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT );
            lp.leftMargin = 50;

            //逐条加载详情信息:
            OrderItem item;
            for( int i=0; i<order.items.size(); i++ ) {

                item = order.items.get(i);

                TextView detailText = new TextView( orderActivity );
                detailText.setTextColor( orderActivity.getResources().getColor(R.color.detail_content));
                detailText.setText(  orderActivity.getString(R.string.content_detail_header)
                        + item.itemName + " ( " + item.itemCount + " ) :  " + item.itemAmount
                        + orderActivity.getString(R.string.pay_unit) );

                orderDetailLayout.addView(detailText, lp);
            }

            //添加配送信息文本框:
            TextView expressText = new TextView( orderActivity );
            expressText.setText(orderActivity.getString(R.string.content_detail_header)
                    + orderActivity.getString(R.string.content_express_price)
                    + " : " + order.expressPrice
                    + orderActivity.getString(R.string.pay_unit));
            expressText.setTextColor( orderActivity.getResources().getColor(R.color.detail_content));
            orderDetailLayout.addView( expressText, lp );

            //结算页不显示操作命令列表：
            if( order.status == OrderActivity.ORDER_TYPE_BALANCE )
                orderCommandLayout.setVisibility( view.GONE );
            else orderCommandLayout.setVisibility( view.VISIBLE );

        }else{
            //自动隐藏之前选择项扩展信息区：
            if( orderExtendLayout.getVisibility() == View.VISIBLE )
                orderExtendLayout.setVisibility( View.GONE );
        }
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

        //设置时间控件:
        SetExpressTime(view, order);

        //设置付款信息控件：
        SetPayment(view, order);

        //设置 Action 按钮不同界面展示文字以及对应事件响应处理:
        SetAction(view, order);

        //设置 Cancel 按钮事件响应处理:
        SetCancel( view, order );

        //设置 PhoneCall 按钮事件响应处理：
        SetPhoneCall( view, order );

        //设置选中项扩展信息区显示内容:
        SetSelected( position, view, order );

        //获取对应列表项对应控件：
        TextView txtAddress = (TextView)view.findViewById( R.id.txtAddresss );
        TextView txtContact = (TextView)view.findViewById( R.id.txtContact );

        //配送地址信息：
        txtAddress.setText( order.location + " " + order.address );
        //联系人姓名：
        txtContact.setText( order.userName );

        return view;
    }
}
