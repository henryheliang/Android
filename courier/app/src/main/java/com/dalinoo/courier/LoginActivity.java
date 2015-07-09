package com.dalinoo.courier;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private Button btnLogin;
    private EditText txtName;
    private EditText txtPassword;
    private CheckBox chkAutoLogin;
    private CheckBox chkRememberPassword;

    private RequestQueue mQueue;
    private SharedPreferences sp;
    private String  mURL;

    //初始化页面控件：
    private void init() {

        //加载预配置信息:
        sp = getSharedPreferences( getString(R.string.app_name), MODE_PRIVATE);

        btnLogin = (Button)findViewById( R.id.btn_login );
        txtName = (EditText)findViewById( R.id.txt_login_name );
        txtPassword = (EditText)findViewById( R.id.txt_login_password );
        chkAutoLogin = (CheckBox)findViewById( R.id.chk_auto_login );
        chkRememberPassword = (CheckBox)findViewById( R.id.chk_remember_password );

        //加载HTTP POST 地址信息：
        mURL = getString(R.string.server_url );

        //创建HTTP 网络序列：
        mQueue = Volley.newRequestQueue(this);
    }

    //读取登录预置参数:
    private void loadPrfile() {

        //用户名：
        txtName.setText( sp.getString( getString(R.string.profile_username), "" ) );
        //密码:
        txtPassword.setText( sp.getString( getString(R.string.profile_password), "" ) );
        //自动登录：
        chkAutoLogin.setChecked( sp.getBoolean( getString(R.string.profile_autologin), false ) );
        //记住密码：
        chkRememberPassword.setChecked( sp.getBoolean( getString(R.string.profile_rememberpassword), false ) );

        //检查是否记住密码：
        if( !chkRememberPassword.isChecked() ){
            //清空密码框内容：
            txtPassword.setText( "" );
            //设定显示为未选中：
            chkAutoLogin.setChecked( false );
        }

        //检查是否为自动登录:
        if( chkAutoLogin.isChecked() ) {
            //检查用户名和密码是否为空：
            if( txtName.getText().toString().equals( "" ) || txtPassword.getText().toString().equals( "" ) ) {
                //清空密码：
                txtPassword.setText( "");
                //清空用户名：
                txtName.setText( "" );
                //设定记录密码框为未选中：
                chkRememberPassword.setChecked( false );
                //设定自动登录框为未选中：
                chkAutoLogin.setChecked( false );
            }
        }
    }

    //存储登录预置参数:
    private void saveProfile() {


        SharedPreferences.Editor editor = sp.edit();

        //存储用户名：
        editor.putString( getString(R.string.profile_username), txtName.getText().toString() );
        //密码：
        editor.putString( getString(R.string.profile_password), txtPassword.getText().toString() );
        //自动登录：
        editor.putBoolean( getString(R.string.profile_autologin),chkAutoLogin.isChecked() );
        //记住密码：
        editor.putBoolean( getString(R.string.profile_rememberpassword), chkRememberPassword.isChecked() );

        //提交修改存储数据:
        editor.commit();

    }

    //发起HTTP POST 登录请求:
    private void postRequest() {

        //发送数据过程中禁用发送按钮避免重复发送请求：
        btnLogin.setEnabled( false );

        //发送请求：
        StringRequest stringRequest = new StringRequest(Request.Method.POST, mURL,
                new Response.Listener<String>() {
                    //处理数据响应：
                    @Override
                    public void onResponse( String response ){
                        onReceiveData( response );
                    }
                },new Response.ErrorListener() {
            //处理网络异常：
            @Override
            public void onErrorResponse( VolleyError error ) {
                Log.e("TAG", error.getMessage(), error );
                Toast.makeText( getApplicationContext(), "网络异常，请稍后重试！",  Toast.LENGTH_SHORT ).show();

                //恢复登录按钮可用：
                btnLogin.setEnabled( true );
            }
        }) {
            //添加POST参数列表：
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put(getString(R.string.url_command), getString(R.string.url_login) );
                map.put(getString(R.string.profile_username), txtName.getText().toString() );
                map.put(getString(R.string.profile_password), txtPassword.getText().toString() );
                return map;
            }
        };

        //添加到HTTP请求序列中:
        mQueue.add(stringRequest);
    }

    //响应发起登录事件:
    private void onLogin() {

        //检查用户名和密码是否为空：
        if( txtName.getText().toString().equals("") || txtPassword.getText().toString().equals( "" ) ){

            Toast.makeText(this, "用户名或密码不能为空！", Toast.LENGTH_SHORT).show();
            return;
        }

        //发送登录请求：
        postRequest();
    }

    //响应HTTP POST 数据返回：
    private void onReceiveData( String str) {

        //恢复登录按钮可用：
        btnLogin.setEnabled( true );

        try{
            //解析JSON登录数据包:
            JSONObject json = new JSONObject( str );
            String strName = json.getString( getString(R.string.url_command) );
            JSONObject subJson = json.getJSONObject( getString(R.string.url_data) );
            final String strID = subJson.getString( getString(R.string.url_loginid) );

            Toast.makeText( this, "登录成功", Toast.LENGTH_SHORT).show();
            saveProfile();

            //将用户ID信息返回主窗口：
            Intent intent = new Intent();
            intent.setClass( this, OrderActivity.class );
            intent.putExtra( getString(R.string.url_loginid), strID );
            this.setResult( Activity.RESULT_OK, intent );

            //关闭当前窗口：
            this.finish();

        } catch ( JSONException e ) {

            //取消自动登录框选中状态：
            chkAutoLogin.setChecked( false );

            //统一处理用户名或密码错误返回信息：
            Toast.makeText( this, "用户名或密码错误，请重新输入！",Toast.LENGTH_SHORT ).show();
            System.out.println("Json parse error");
            e.printStackTrace();

            }

    }

    //监听 Activity 按键事件：
    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event ) {

        //用户点击返回按钮直接退出应用：
        if( keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 ) {
            this.setResult(Activity.RESULT_CANCELED);
        }

        return super.onKeyDown( keyCode, event );
    }

    //Activity 创建事件：
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //初始化控件：
        init();

        //加载预配置信息:
        loadPrfile();

        //监听登录按钮事件：
        btnLogin.setOnClickListener( new View.OnClickListener() {
            @Override public void onClick( View view ){
                onLogin();
            }
        });

        //检查是否需要自动登录：
        if( chkAutoLogin.isChecked() ) {
            onLogin();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
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
