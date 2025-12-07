package com.flight.airchat;

import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.tencent.imsdk.v2.*;

import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        // 1. 从即时通信 IM 控制台获取应用 SDKAppID。
// 2. 初始化 config 对象。
        V2TIMSDKConfig config = new V2TIMSDKConfig();
// 3. 指定 log 输出级别。
        config.setLogLevel(V2TIMSDKConfig.V2TIM_LOG_INFO);
// 4. 添加 V2TIMSDKListener 的事件监听器，sdkListener 是 V2TIMSDKListener 的实现类，如果您不需要监听 IM SDK 的事件，这个步骤可以忽略。

// 5. 初始化 IM SDK，调用这个接口后，可以立即调用登录接口。
        V2TIMManager.getInstance().initSDK(this, Integer.parseInt("1600013134"), config);

        //String userID = "1997398474248781826";

        new Thread(() -> {
            try {
                Map<String, String> result = HttpRequestUtil.sendUserinfoRequest("ze7X7ZwyGDkJ6D4a7ZHinBR6DJoM715T");
                String userID = result.get("id");
                String userSig = result.get("userSig");

                // 切换到主线程显示结果
                new Handler(Looper.getMainLooper()).post(() -> {
                    Log.i("imsdk", "获取到的 UserID: " + userID);
                    Log.i("imsdk", "获取到的 UserSig: " + userSig);
                });

                // 在新线程中执行登录（IM SDK登录操作）
                new Thread(() -> V2TIMManager.getInstance().login(userID, userSig, new V2TIMCallback() {
                    @Override
                    public void onSuccess() {
                        Log.i("imsdk", "IM登录成功");
                        // 登录成功后再切换到主线程
                        new Handler(Looper.getMainLooper()).post(() -> {
                            // 更新UI显示登录成功
                            // 例如：textView.setText("登录成功");
                        });
                        // 发送消息（可以在任何线程）
                        sendMessage();
                    }

                    @Override
                    public void onError(int code, String desc) {
                        Log.e("imsdk", "IM登录失败, code:" + code + ", desc:" + desc);
                        // 切换到主线程显示错误
                        new Handler(Looper.getMainLooper()).post(() -> {
                            // 显示错误信息
                            // 例如：Toast.makeText(context, "登录失败：" + desc, Toast.LENGTH_SHORT).show();
                        });
                    }
                })).start();

            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> Log.e("imsdk", "请求失败: " + e.getMessage()));
            } catch (Exception e) {
                Log.e("imsdk", "异常: " + e.getMessage());
            }
        }).start();



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    // 发送消息的方法
    private void sendMessage() {
        String targetUserID = "1910213639351259138";
        String messageText = "单聊文本消息12111111111";
        V2TIMManager.getInstance().sendC2CTextMessage(messageText, targetUserID, new V2TIMValueCallback<>() {
            @Override
            public void onSuccess(V2TIMMessage message) {
                Log.i("imsdk", "发送单聊文本消息成功, msgID: " + message.getMsgID());
                // 如果需要更新UI，切换到主线程
                new Handler(Looper.getMainLooper()).post(() -> {
                    // 更新UI显示发送成功
                });
            }

            @Override
            public void onError(int code, String desc) {
                Log.e("imsdk", "发送消息失败, code: " + code + ", desc: " + desc);
            }
        });
    }

}