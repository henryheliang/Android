package com.dalinoo.courier;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.umeng.message.PushAgent;
import com.umeng.message.UmengRegistrar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderActivity extends AppCompatActivity {

    private TabHost         tabHost;
    private ListView        listView;
    private List<Order>     orders;
    private OrderAdapter    adapter;
    private RequestQueue    mQueue;
    private String          strURL;
    private Context         context;
    private String          loginID;
    private int             mOrderType;


    private void init() {

        listView = (ListView)findViewById( R.id.listOrder );
        orders = new ArrayList<Order>();
        adapter = new OrderAdapter( this, R.layout.activity_order_item, orders );
        adapter.orderActivity = this;
        listView.setAdapter( adapter );

        //初始化Tab选项：
        tabHost = (TabHost) this.findViewById(R.id.tabHost);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("tab_1").setContent(R.id.tab1).setIndicator("未接订单"));
        tabHost.addTab(tabHost.newTabSpec("tab_2").setContent(R.id.tab1).setIndicator("已接订单"));
        tabHost.setCurrentTab(0);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if( "tab_1".equals(tabId) ) mOrderType = 1;
                else if( "tab_2".equals(tabId) ) mOrderType = 2;
                loadOrderList( mOrderType );

                setTitle( "hello!");
            }
        });

        context = this.getBaseContext();
        strURL = "http://dalinoo.com/dl/appdata.php";
        mQueue = Volley.newRequestQueue(context);
    }

    private void onReceiveData( String data ) {

        try{
            JSONObject json = new JSONObject( data );
            String strCommand = json.getString("command");

            if( strCommand.equals( "updateorder") ) {
                loadOrderList( mOrderType );
                return;
            }

            if( strCommand.equals( "orderlist" ) ) {

                orders.removeAll(orders);

                if( json.get("data") == JSONObject.NULL ){
                    adapter.notifyDataSetChanged();
                    return;
                }

                JSONArray subJsons = json.getJSONArray( "data" );
                for( int i=0; i<subJsons.length(); i++ ) {
                    JSONObject item = subJsons.getJSONObject( i );
                    Order newOrder =  new Order();
                    newOrder.mobileNo = item.getString("mobile_no");
                    newOrder.orderID = item.getString("order_id");
                    newOrder.payment = item.getDouble( "order_amount");
                    newOrder.payType = item.getInt("order_type");
                    newOrder.userName = item.getString("user_name");

                    DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss");
                    newOrder.orderTime = dateFormat.parse( item.getString("order_time") );
                    newOrder.expressTime = dateFormat.parse( item.getString("send_time") );
                    newOrder.location = item.getString("location_name");
                    newOrder.address = item.getString("address_info");
                    newOrder.status = mOrderType;

                    orders.add( newOrder );
                }

                adapter.notifyDataSetChanged();
            }


        } catch ( JSONException e ) {

            Toast.makeText( this, "网络异常，请稍后重试！",Toast.LENGTH_SHORT ).show();
            System.out.println("Json parse error");
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void loadOrderList( final int nType ) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, strURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse( String response ){
                        Log.d("TAG", response);
                        onReceiveData( response );
                    }
                },new Response.ErrorListener() {
            @Override
            public void onErrorResponse( VolleyError error ) {
                Log.e("TAG", error.getMessage(), error );
                Toast.makeText( getApplicationContext(), "网络异常，请稍后重试！",  Toast.LENGTH_SHORT ).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("command", "orderlist");
                map.put("orderstatus", String.valueOf(nType) );
                map.put("loginid", loginID );
                return map;
            }
        };
        mQueue.add(stringRequest);
    }

    public void updateOrder( final String strID, final int nStatus ) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, strURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse( String response ){
                        Log.d("TAG", response);
                        onReceiveData( response );
                    }
                },new Response.ErrorListener() {
            @Override
            public void onErrorResponse( VolleyError error ) {
                Log.e("TAG", error.getMessage(), error );
                Toast.makeText( getApplicationContext(), "网络异常，请稍后重试！",  Toast.LENGTH_SHORT ).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("command", "updateorder");
                map.put("orderstatus", String.valueOf(nStatus) );
                map.put("loginid", loginID );
                map.put( "orderid", strID );
                return map;
            }
        };
        mQueue.add(stringRequest);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        PushAgent mPushAgent = PushAgent.getInstance(this);
        mPushAgent.enable();

        String device_token = UmengRegistrar.getRegistrationId(this);
        Log.d( "TAG", device_token );

        //初始化页面：
        init();

        //默认跳转到登录页面：
        Intent intent = new Intent();
        intent.setClass( this, LoginActivity.class );
        startActivityForResult(intent, 0);

    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );

        if( resultCode == Activity.RESULT_OK ) {
            loginID = data.getStringExtra("loginid");
            Toast.makeText(this, "用户ID号: " + data.getStringExtra("loginid"), Toast.LENGTH_SHORT ).show();
            mOrderType = 1;
            loadOrderList( mOrderType );

        }else if(resultCode == Activity.RESULT_CANCELED ) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_order, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
