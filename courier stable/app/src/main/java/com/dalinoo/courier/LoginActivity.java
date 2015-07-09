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
import com.umeng.message.PushAgent;

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

    private Context context;
    private RequestQueue mQueue;
    private SharedPreferences sp;
    private String  strURL;

    private void init() {

        context = this.getBaseContext();
        sp = context.getSharedPreferences("courier", MODE_PRIVATE);

        btnLogin = (Button)findViewById( R.id.btn_login );
        txtName = (EditText)findViewById( R.id.txt_login_name );
        txtPassword = (EditText)findViewById( R.id.txt_login_password );
        chkAutoLogin = (CheckBox)findViewById( R.id.chk_auto_login );
        chkRememberPassword = (CheckBox)findViewById( R.id.chk_remember_password );

        strURL = "http://dalinoo.com/dl/appdata.php";
        mQueue = Volley.newRequestQueue(context);
    }

    private void loadPrfile() {

        txtName.setText( sp.getString( "username", "" ) );
        txtPassword.setText( sp.getString( "password", "" ) );

        chkAutoLogin.setChecked( sp.getBoolean( "isautologin", false ) );
        chkRememberPassword.setChecked( sp.getBoolean( "isrememberpassword", false ) );

        if( !chkRememberPassword.isChecked() ){
            txtPassword.setText( "" );
            chkAutoLogin.setChecked( false );
        }

        if( chkAutoLogin.isChecked() ) {
            if( txtName.getText().toString().equals( "" ) || txtPassword.getText().toString().equals( "" ) ) {
                txtPassword.setText( "");
                txtName.setText( "" );
                chkRememberPassword.setChecked( false );
                chkAutoLogin.setChecked( false );
            }
        }
    }

    private void saveProfile() {

        SharedPreferences.Editor editor = sp.edit();
        editor.putString( "username", txtName.getText().toString() );
        editor.putString( "password", txtPassword.getText().toString() );
        editor.putBoolean( "isautologin",chkAutoLogin.isChecked() );
        editor.putBoolean( "isrememberpassword", chkRememberPassword.isChecked() );
        editor.commit();

    }

    private void postRequest() {

        btnLogin.setEnabled( false );

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
                    btnLogin.setEnabled( true );
                }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("command", "login");
                map.put("username", txtName.getText().toString() );
                map.put("password", txtPassword.getText().toString() );
                return map;
            }
        };
        mQueue.add(stringRequest);
    }

    private void onLogin() {

        if( txtName.getText().toString().equals("") || txtPassword.getText().toString().equals( "" ) ){

            Toast.makeText(this, "用户名或密码不能为空！", Toast.LENGTH_SHORT).show();
            return;
        }

        //发送登录请求：
        postRequest();
    }

    private void onReceiveData( String str) {

        btnLogin.setEnabled( true );

        try{
            JSONObject json = new JSONObject( str );
            String strName = json.getString("command");
            JSONObject subJson = json.getJSONObject("data");
            final String strID = subJson.getString( "loginid");

            Toast.makeText( this, "登录成功", Toast.LENGTH_SHORT).show();
            saveProfile();


            Intent intent = new Intent();
            intent.setClass( this, OrderActivity.class );
            intent.putExtra( "loginid", strID );
            this.setResult( Activity.RESULT_OK, intent );
            this.finish();

        } catch ( JSONException e ) {

            chkAutoLogin.setChecked( false );
            Toast.makeText( this, "用户名或密码错误，请重新输入！",Toast.LENGTH_SHORT ).show();
            System.out.println("Json parse error");
            e.printStackTrace();

            }

    }

    @Override public boolean onKeyDown( int keyCode, KeyEvent event ) {

        if( keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 ) {
            this.setResult(Activity.RESULT_CANCELED);
        }

        return super.onKeyDown( keyCode, event );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        PushAgent.getInstance(this).onAppStart();

        init();

        loadPrfile();

        btnLogin.setOnClickListener( new View.OnClickListener() {
            @Override public void onClick( View view ){
                onLogin();
            }
        });

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
