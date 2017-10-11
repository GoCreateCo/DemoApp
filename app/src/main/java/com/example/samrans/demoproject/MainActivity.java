package com.example.samrans.demoproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    LoginButton loginButton;
    private CallbackManager callbackManager;
    private AccessTokenTracker accessTokenTracker;
    private AccessToken accessToken;
    private ProfileTracker profileTracker;
    private Context mContext;
    private RelativeLayout rel_FB, rel_GPlus;
    private ProfilePictureView friendProfilePicture;
    private TextView tv_fb_name, tv_gplus_name;
    ImageView iv_gplus_profile, iv_fb_profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        init();

        callbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions(Arrays.asList("public_profile", "email"));
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                // Set the access token using
                // currentAccessToken when it's loaded or set.
                Toast.makeText(mContext, "onCurrentAccessTokenChanged", Toast.LENGTH_SHORT).show();

            }
        };
        // If the access token is available already assign it.
        accessToken = AccessToken.getCurrentAccessToken();

        details();
        if (accessToken != null) {
            friendProfilePicture.setProfileId(accessToken.getUserId());
            Picasso.with(mContext)
                    .load("https://graph.facebook.com/" + accessToken.getUserId() + "/picture?type=small")
                    .into(iv_fb_profile);
        }

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(
                    Profile oldProfile,
                    Profile currentProfile) {
                // App code
                Toast.makeText(mContext, "onCurrentProfileChanged", Toast.LENGTH_SHORT).show();

            }
        };


        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(mContext, "onSuccess", Toast.LENGTH_SHORT).show();
                friendProfilePicture.setProfileId(loginResult.getAccessToken().getUserId());
                Picasso.with(mContext)
                        .load("https://graph.facebook.com/" + loginResult.getAccessToken().getUserId() + "/picture?type=small")
                        .into(iv_fb_profile);

                Bitmap bitmap = getFacebookProfilePicture(loginResult.getAccessToken().getUserId());
                details();
            }

            @Override
            public void onCancel() {
                Toast.makeText(mContext, "onCancel", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(mContext, "onError", Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void details() {
        Bundle params = new Bundle();
        params.putString("fields", "name,id,email,picture.type(large)");
        new GraphRequest(AccessToken.getCurrentAccessToken(), "me", params, HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        if (response != null) {
                            try {
                                JSONObject data = response.getJSONObject();
                                if (data.has("picture")) {
                                    String profilePicUrl = data.getJSONObject("picture").getJSONObject("data").getString("url");
                                    // set profilePic bitmap to imageview
                                    if (data.has("name") && data.has("email"))tv_fb_name.setText(data.getString("name") +
                                     "\n"+data.getString("email"));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).executeAsync();
    }

    private void init() {
        loginButton = (LoginButton) findViewById(R.id.login_button);
        friendProfilePicture = (ProfilePictureView) findViewById(R.id.friendProfilePicture);
        rel_FB = (RelativeLayout) findViewById(R.id.rel_FB);
        rel_GPlus = (RelativeLayout) findViewById(R.id.rel_GPlus);

        tv_fb_name = (TextView) findViewById(R.id.tv_fb_name);
        tv_gplus_name = (TextView) findViewById(R.id.tv_gplus_name);

        iv_gplus_profile = (ImageView) findViewById(R.id.iv_gplus_profile);
        iv_fb_profile = (ImageView) findViewById(R.id.iv_fb_profile);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
        profileTracker.stopTracking();

    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    if (rel_FB.getVisibility() != View.VISIBLE) {
                        rel_FB.setVisibility(View.VISIBLE);
                        rel_GPlus.setVisibility(View.GONE);
                    }
//                    LoginManager.getInstance().logInWithReadPermissions(MainActivity.this, Arrays.asList("public_profile","email"));

                    return true;
                case R.id.navigation_dashboard:
                    if (rel_GPlus.getVisibility() != View.VISIBLE) {
                        rel_GPlus.setVisibility(View.VISIBLE);
                        rel_FB.setVisibility(View.GONE);
                    }

                    return true;
                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }

    };

    public static Bitmap getFacebookProfilePicture(String userID) {
        Bitmap bitmap = null;
        try {
            URL imageURL = new URL("https://graph.facebook.com/" + userID + "/picture?type=large");
            bitmap = BitmapFactory.decodeStream(imageURL.openConnection().getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }

}
