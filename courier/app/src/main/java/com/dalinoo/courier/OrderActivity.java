package com.dalinoo.courier;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

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

    private TabHost tabHost;
    private TabWidget tabWidget;
    private ListView listView;
    private List<Order> orders;
    private OrderAdapter adapter;
    private RequestQueue mQueue;
    private GestureDetector mDetector;
    private String mURL;
    private String loginID;
    private int mOrderType;
    public int mCurrentPostion;
    public static final int ORDER_TYPE_NEW = 1;
    public static final int ORDER_TYPE_ACCEPT = 2;
    public static final int ORDER_TYPE_EXPRESS = 3;
    public static final int ORDER_TYPE_CANCEL = 0;
    private static final int INVALID_POSITION = -1;

    //初始化变量以及控件：
    private void init() {

        //初始化手势控件:
        mDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                   float velocityY) {

                if(e1.getX()-e2.getX()>120){ //向左滑动:
                    if( mOrderType < ORDER_TYPE_EXPRESS ) {
                        ++mOrderType;
                        tabHost.setCurrentTab( mOrderType-1 );
                    }
                } else if (e1.getX()-e2.getX()<-120){ //向右滑动:
                    if( mOrderType > ORDER_TYPE_NEW ) {
                        --mOrderType;
                        tabHost.setCurrentTab( mOrderType-1 );
                    }
                }

                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                    float distanceY) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

        });

        //初始化列表：
        listView = (ListView) findViewById(R.id.listOrder);
        orders = new ArrayList<Order>();

        //让主视图手势控件接管列表手势消息处理:
        listView.setOnTouchListener( new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mDetector.onTouchEvent(event);
                return false;
            }
        });

        //初始化列表适配器：
        adapter = new OrderAdapter(this, R.layout.activity_order_item, orders);
        adapter.orderActivity = this;

        //列表加载适配器：
        listView.setAdapter(adapter);

        //设定初始化用户列表选择位置：
        mCurrentPostion = INVALID_POSITION;

        //监听列表项点击事件：
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //记录用户选择列表项：
                mCurrentPostion = position;

                //通知适配器更新列表数据：
                adapter.notifyDataSetChanged();
            }
        });

        //初始化Tab控件并处理切换事件:
        initTabHost();

        //设定服务器HTTP URL 字符串：
        mURL = getString(R.string.server_url);

        //添加Sever交互服务：
        mQueue = Volley.newRequestQueue(this);

    }

    //初始化 TabHost 控件以及处理相关事件:
    private void initTabHost() {

        //初始化Tab选项：
        tabHost = (TabHost) this.findViewById(R.id.tabHost);
        tabHost.setup();
        tabWidget = tabHost.getTabWidget();

        //创建两个Tab项：
        tabHost.addTab(tabHost.newTabSpec("tab_1").setContent(R.id.tab1).setIndicator(getString(R.string.tab_1)));
        tabHost.addTab(tabHost.newTabSpec("tab_2").setContent(R.id.tab1).setIndicator(getString(R.string.tab_2)));
        tabHost.addTab(tabHost.newTabSpec("tab_3").setContent(R.id.tab1).setIndicator(getString(R.string.tab_3)));

        //默认定位到第一个Tab项:
        tabHost.setCurrentTab(0);
        mOrderType = ORDER_TYPE_NEW;

        /*
        //修改Tab显示字体大小
        for (int i =0; i < tabWidget.getChildCount(); i++) {
            TextView tv = (TextView) tabWidget.getChildAt(i).findViewById(android.R.id.title);
            tv.setTextSize( getResources().getDimension(R.dimen.tab_fontsize ) );
        }
        */

        //监听处理Tab点击事件:
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {

                if ("tab_1".equals(tabId)) mOrderType = ORDER_TYPE_NEW;
                else if ("tab_2".equals(tabId)) mOrderType = ORDER_TYPE_ACCEPT;
                else if ("tab_3".equals(tabId)) mOrderType = ORDER_TYPE_EXPRESS;
                loadOrderList(mOrderType);
            }
        });

    }

    //处理所有 URL POST 返回数据解析
    private void onReceiveData( String data ) {

        try{
            //将数据按照JSON格式进行自动解析：
            JSONObject json = new JSONObject( data );
            String strCommand = json.getString( getString(R.string.url_command));

            //准备Tab项文字信息：
            String strContent = "";
            switch ( mOrderType ) {
                case ORDER_TYPE_NEW:
                    strContent = getString( R.string.tab_1 );
                    break;
                case ORDER_TYPE_ACCEPT:
                    strContent = getString( R.string.tab_2 );
                    break;
                case ORDER_TYPE_EXPRESS:
                    strContent = getString( R.string.tab_3 );
                    break;
                default:
            }

            //处理"update"命令事务：
            if( strCommand.equals( getString( R.string.url_update ) ) ) {

                //发起重新刷新列表请求：
                loadOrderList( mOrderType );
                return;
            }

            //处理"list"命令事务:
            if( strCommand.equals( getString(R.string.url_list) ) ) {

                //清除所有列表项并重置用户现在位置：
                orders.removeAll(orders);
                mCurrentPostion = INVALID_POSITION;

                //返回0条记录时：
                if( json.get( getString(R.string.url_data) ) == JSONObject.NULL ){

                    //通知适配器更新数据：
                    adapter.notifyDataSetChanged();

                    //设定当前Tab项文字
                    ((TextView)tabWidget.getChildAt(mOrderType-1).findViewById(android.R.id.title)).setText(strContent);
                    //Toast.makeText(this, strContent + ": 0", Toast.LENGTH_SHORT ).show();
                    return;
                }

                //解析JSON数据集：
                JSONArray subJsons = json.getJSONArray( "data" );
                for( int i=0; i<subJsons.length(); i++ ) {

                    //读取一项数据集：
                    JSONObject item = subJsons.getJSONObject( i );

                    //填充订单实例数据项：
                    Order newOrder =  new Order();
                    newOrder.mobileNo = item.getString("mobile_no");
                    newOrder.orderID = item.getString("order_id");
                    newOrder.payment = item.getDouble( "order_amount");
                    newOrder.payType = item.getInt("order_type");
                    newOrder.userName = item.getString("user_name");
                    //设定时间字符串格式：
                    DateFormat dateFormat = new SimpleDateFormat(  getString(R.string.date_format) );
                    newOrder.orderTime = dateFormat.parse( item.getString("order_time") );
                    newOrder.expressTime = dateFormat.parse( item.getString("send_time") );
                    newOrder.location = item.getString("location_name");
                    newOrder.address = item.getString("address_info");
                    newOrder.status = mOrderType;

                    //设定配送时间字符串：
                    newOrder.setTimeString();

                    //订单实例添加到订单列表：
                    orders.add( newOrder );
                }

                //通知适配器刷新列表：
                adapter.notifyDataSetChanged();

                //更新Tab项显示带有订单数量的动态标题：
                ((TextView)tabWidget.getChildAt(mOrderType-1).findViewById(android.R.id.title))
                        .setText(strContent + " (" + subJsons.length() + ")");
                //Toast.makeText(this, strContent + ": " + subJsons.length() , Toast.LENGTH_SHORT ).show();
            }

        } catch ( JSONException e ) {

            //捕获JSON数据解析异常：
            Toast.makeText( this, "数据包解析错误，请稍后重试！",Toast.LENGTH_SHORT ).show();
            System.out.println("Json parse error");
            e.printStackTrace();
        } catch (ParseException e) {

            //捕获时间字符串解析异常：
            Toast.makeText( this, "时间字符串解析错误，请稍后重试！",Toast.LENGTH_SHORT ).show();
            e.printStackTrace();
        }
    }

    //发送HTTP POST 刷新订单请求：
    private void loadOrderList( final int nType ) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, mURL,
                new Response.Listener<String>() {

                    //监听 HTTP POST - REPONSE 事件：
                    @Override
                    public void onResponse( String response ){

                        //处理HTTP POST 返回数据响应：
                        onReceiveData( response );
                    }
                },new Response.ErrorListener() {

            //监听 HTTP POST - ERROR 事件：
            @Override
            public void onErrorResponse( VolleyError error ) {
                Log.e("TAG", error.getMessage(), error );
                Toast.makeText( getApplicationContext(), "网络异常，请稍后重试！",  Toast.LENGTH_SHORT ).show();
            }
        }) {
            //添加Post参数列表：
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put( getString(R.string.url_command), getString(R.string.url_list) );
                map.put( getString(R.string.url_status), String.valueOf(nType) );
                map.put( getString(R.string.url_loginid), loginID );
                return map;
            }
        };

        //将请求添加到HTTP请求序列中：
        mQueue.add(stringRequest);
    }

    //发送HTTP POST 更新订单请求：
    public void updateOrder( final String strID, final int nStatus ) {

        StringRequest stringRequest = new StringRequest(Request.Method.POST, mURL,
                new Response.Listener<String>() {

                    //监听 HTTP POST - RESPONSE 事件：
                    @Override
                    public void onResponse( String response ){
                        onReceiveData( response );
                    }
                },new Response.ErrorListener() {

            //监听 HTTP POST - ERROR 事件：
            @Override
            public void onErrorResponse( VolleyError error ) {
                Log.e("TAG", error.getMessage(), error );
                Toast.makeText( getApplicationContext(), "网络异常，请稍后重试！",  Toast.LENGTH_SHORT ).show();
            }
        }) {

            //添加 POST 参数列表：
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put( getString(R.string.url_command), getString(R.string.url_update) );
                map.put( getString(R.string.url_status), String.valueOf(nStatus) );
                map.put( getString(R.string.url_loginid), loginID );
                map.put( getString(R.string.url_orderid), strID );
                return map;
            }
        };

        //将请求添加到HTTP发送序列中：
        mQueue.add(stringRequest);
    }

    //响应Activity 创建事件：
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //将Layout与Activiy实例捆绑:
        setContentView(R.layout.activity_order);

        //注册友盟消息推送接口：
        PushAgent mPushAgent = PushAgent.getInstance(this);
        mPushAgent.enable();

        //参看当前设备 Device-Token 信息：
        //String device_token = UmengRegistrar.getRegistrationId(this);
        //Log.d( "TAG", "Device Token: " + device_token );

        //初始化页面：
        init();

        //默认跳转到登录页面：
        Intent intent = new Intent();
        intent.setClass( this, LoginActivity.class );
        startActivityForResult(intent, 0);

    }

    @Override
    public boolean onTouchEvent( MotionEvent event ) {
        return mDetector.onTouchEvent( event );
    }

    //响应页面跳转返回事件:
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );

        //成功返回存储当前登录者 ID 信息:
        if( resultCode == Activity.RESULT_OK ) {
            loginID = data.getStringExtra("loginid");
            loadOrderList( mOrderType );

        }else if(resultCode == Activity.RESULT_CANCELED ) {

            //失败返回意味着登录失败或者取消登录，直接退出应用：
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
