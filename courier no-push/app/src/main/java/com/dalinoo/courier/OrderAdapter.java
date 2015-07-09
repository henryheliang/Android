package com.dalinoo.courier;

import android.content.Context;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout view = null;

        if( convertView == null) {
            view = (LinearLayout)mInflater.inflate(  mResouce, null );

        }else {
            view = (LinearLayout)convertView;
        }

        final Order order = getItem( position );

        TextView txtExpressTime = (TextView)view.findViewById( R.id.txtExpressTime );
        TextView txtAddress = (TextView)view.findViewById( R.id.txtAddresss );
        TextView txtContact = (TextView)view.findViewById( R.id.txtContact );
        TextView txtPayment = (TextView)view.findViewById( R.id.txtPayment );
        Button   btnAction = (Button)view.findViewById( R.id.btnAction );

        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
        txtExpressTime.setText( dateFormat.format(order.expressTime) );
        txtAddress.setText( order.location + " " + order.address );
        txtContact.setText( order.userName + "" + order.mobileNo );

        String strPayment = String.valueOf( order.payment );
        if( order.payType == 1 )  strPayment += " （在线支付)";
        else if( order.payType == 2 ) strPayment += " (货到付款)";
        txtPayment.setText( strPayment );

        if( order.status == 1 ) btnAction.setText( "开始" );
        else if( order.status == 2 ) btnAction.setText( "完成" );

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d( "TAG", order.expressTime.toString() );

                Intent intent = new Intent(Intent.ACTION_CALL);
                Uri data = Uri.parse("tel:" + order.mobileNo);
                intent.setData(data);
                orderActivity.startActivity(intent);
            }
        });

        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                orderActivity.updateOrder( order.orderID, order.status+1 );
                Log.d( "TAG", order.orderID );
            }
        });

        return view;
    }
}
