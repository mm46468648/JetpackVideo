package com.example.mjetpack.ui.login;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.lib_common.global.AppGlobals;
import com.example.lib_network.ApiResponse;
import com.example.lib_network.ApiService;
import com.example.lib_network.JsonCallback;
import com.example.lib_network.cache.CacheManager;
import com.example.mjetpack.model.User;

public class UserManager {
    private static final String KEY_CACHE_USER = "cache_user";
    private static UserManager mUserManager = new UserManager();
    private MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private User mUser;

    public static UserManager get() {
        return mUserManager;
    }

    private UserManager() {

//        mUser = new User();
//        mUser.userId = 19930521;
//        mUser.name = "mm";

        User cache = (User) CacheManager.getCache(KEY_CACHE_USER);
        if (cache != null && cache.expires_time > System.currentTimeMillis()) {
            mUser = cache;
        }
    }

    public void save(User user) {
        mUser = user;
        CacheManager.save(KEY_CACHE_USER, user);
        if (userLiveData.hasObservers()) {
            userLiveData.postValue(user);
        }
    }

    public LiveData<User> login(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return userLiveData;
    }

    public boolean isLogin() {
        return mUser != null;
//        return mUser == null ? false : mUser.expires_time > System.currentTimeMillis();
    }

    public User getUser() {
        return isLogin() ? mUser : null;
    }

    public long getUserId() {
        return isLogin() ? mUser.userId : 0;
    }


    public LiveData<User> refresh() {
        if (!isLogin()) {
            return login(AppGlobals.getApplication());
        }
        MutableLiveData<User> liveData = new MutableLiveData<>();
        ApiService.get("/user/query")
                .addParam("userId", getUserId())
                .execute(new JsonCallback<User>() {
                    @Override
                    public void onSuccess(ApiResponse<User> response) {
                        save(response.body);
                        liveData.postValue(getUser());
                    }

                    @Override
                    public void onError(ApiResponse<User> response) {
                        ArchTaskExecutor.getMainThreadExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AppGlobals.getApplication(), response.message, Toast.LENGTH_SHORT).show();
                            }
                        });

                        liveData.postValue(null);
                    }
                });
        return liveData;
    }

    public void logout() {
        CacheManager.delete(KEY_CACHE_USER, mUser);
        mUser = null;
    }
}
