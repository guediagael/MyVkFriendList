package com.etton.testtask.myvkfriendlist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKList;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    private String  sc = VKScope.FRIENDS;
    private ListView lv ;

    int numberOfCachedElements=0;
    public static final String CachedFriends= "cachedFriends";
    public static final String LoginFlag = "loginFlag";
    public static final String mLog="log";
    public static final String mVklog="vklog";

    public SharedPreferences loginFlag;
    public SharedPreferences cachedFriends;

    private boolean hasBeenlogged= false;
    public boolean logged=false;

    private Button logout;
    private Button cleanTheCache;

    VKList listClone;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_main);

//        vk certificate
//        String[] fingerprints = VKUtil.getCertificateFingerprint(this, this.getPackageName());
//        System.out.println(Arrays.asList(fingerprints));

        //The login flag is cached
        loginFlag =getSharedPreferences(LoginFlag,MODE_PRIVATE);

        if ((loginFlag.getBoolean(mLog,hasBeenlogged))&& (loginFlag.getBoolean(mVklog,logged))) {
            VKSdk.login(this, sc);

        }
        else {
            cachedFriends=getSharedPreferences(CachedFriends,MODE_PRIVATE);

            //we have to go back in our file containing the cached list of friends
           if (cachedFriends.contains("val1")) {
               ArrayList<String> listClone =new ArrayList<>();
               int size= cachedFriends.getInt("size",0);
               for (int j=0;j<size;j++){
                   listClone.add(cachedFriends.getString("val"+j,null));
               }
               lv = (ListView) findViewById(R.id.listOfFriends);
                ArrayAdapter<String> aA = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_expandable_list_item_1, listClone);
                lv.setAdapter(aA);
                Toast.makeText(getApplicationContext(), "Loaded from the cache", Toast.LENGTH_LONG).show();
           }
            else
                Toast.makeText(getApplicationContext(), "The cache is empty", Toast.LENGTH_LONG).show();
        }
    }




//Listener for the logout button
    public void logout(View view){
        if (VKSdk.isLoggedIn()){
        loginFlag=getSharedPreferences(LoginFlag,MODE_PRIVATE);
        SharedPreferences.Editor editFlag = loginFlag.edit();
        editFlag.clear();
        editFlag.putBoolean(mLog,!hasBeenlogged);
        editFlag.putBoolean(mVklog,!logged);
        editFlag.apply();
        VKSdk.logout();
            logout.setText("login");
        }
        else {

            VKSdk.login(this,sc);

        }

       logout.setText("login");
    }

    //Listener for the cache cleaner
    public void cleanTheCache(View view){
        SharedPreferences cachedFriends = getSharedPreferences(CachedFriends, MODE_PRIVATE);
        SharedPreferences.Editor editor= cachedFriends.edit();
        editor.clear();
        editor.commit();

        lv.setAdapter(null);
        cleanTheCache.setVisibility(View.GONE);
    }



    @Override
    protected void onResume(){

        loginFlag =getSharedPreferences(LoginFlag,MODE_PRIVATE);



        logout=(Button)findViewById(R.id.logout);
        logout.setText("logout");

        cleanTheCache=(Button)findViewById(R.id.clear_the_cache);

        super.onResume();
    }

    @Override
    public void onBackPressed(){

        MainActivity.this.finish();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                lv=(ListView)findViewById(R.id.listOfFriends);
                final VKRequest request = VKApi.friends().get(VKParameters.from(VKApiConst.FIELDS,"user_id,first_name,last_name"));
                request.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);

                        final VKList list =(VKList) response.parsedModel;
                        ArrayAdapter<String> arrayAdapter= new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_expandable_list_item_1, list);
                        lv.setAdapter(arrayAdapter);

                        /*
                          The log in flags are changed when the user has successfully retrieved her friend list.
                          The app is not considered as logged if the user is not able to acces the list of friends.

                         */
                        loginFlag=getSharedPreferences(LoginFlag,MODE_PRIVATE);
                        SharedPreferences.Editor editFlag = loginFlag.edit();
                        editFlag.clear();
                        editFlag.putBoolean(mLog,hasBeenlogged);
                        editFlag.putBoolean(mVklog,logged);
                        editFlag.apply();


                        //The list received by Vk is cloned and saved (cached )

                        listClone =new VKList(list);
                        SharedPreferences cachedFriends = getSharedPreferences(CachedFriends, MODE_PRIVATE);
                        SharedPreferences.Editor editor= cachedFriends.edit();
                        editor.clear();
                        for (int x=0;x<listClone.size();x++){
                            editor.putString("val"+x,(listClone.get(x)).toString());
                        }
                        editor.putInt("size",listClone.size());
                        editor.apply();

                    }

                });

                Toast.makeText(getApplicationContext(),"Успешно :)",Toast.LENGTH_LONG).show();
            }
            @Override
            public void onError(VKError error) {


                    Toast.makeText(getApplicationContext(),"Не удалось получить список :(",Toast.LENGTH_LONG).show();
            }
        }))


        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
