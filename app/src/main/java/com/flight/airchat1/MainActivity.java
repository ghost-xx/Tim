package com.flight.airchat1;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.tencent.imsdk.v2.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    
    // 界面控件声明
    private EditText tokenInput;           // Token输入框
    private Button loginButton;            // 登录按钮
    private EditText targetUserIDInput;    // 目标用户ID输入框
    private EditText messageInput;         // 消息内容输入框
    private Button sendButton;             // 发送文本消息按钮
    private EditText imagePathInput;       // 图片路径输入框
    private Button selectImageButton;      // 选择图片按钮
    private Button sendImageButton;        // 发送图片消息按钮
    private TextView statusText;           // 状态显示文本

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 初始化界面控件
        initViews();
        
        // 设置按钮点击事件监听器
        setupButtonListeners();

        // 初始化IM SDK
        initIMSDK();

        // 设置窗口边距适配
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * 初始化界面控件
     */
    private void initViews() {
        tokenInput = findViewById(R.id.tokenInput);
        loginButton = findViewById(R.id.loginButton);
        targetUserIDInput = findViewById(R.id.targetUserIDInput);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        imagePathInput = findViewById(R.id.imagePathInput);
        selectImageButton = findViewById(R.id.selectImageButton);
        sendImageButton = findViewById(R.id.sendImageButton);
        statusText = findViewById(R.id.statusText);
        // 读取保存的Token
        String savedToken = getSharedPreferences("user_prefs", MODE_PRIVATE)
                .getString("user_token", "");
        if (!TextUtils.isEmpty(savedToken)) {
            tokenInput.setText(savedToken);
        }
    }

    /**
     * 初始化IM SDK
     */
    private void initIMSDK() {
        // 1. 从即时通信 IM 控制台获取应用 SDKAppID
        // 2. 初始化 config 对象
        V2TIMSDKConfig config = new V2TIMSDKConfig();
        // 3. 指定 log 输出级别
        config.setLogLevel(V2TIMSDKConfig.V2TIM_LOG_INFO);
        // 4. 添加 V2TIMSDKListener 的事件监听器
        // 5. 初始化 IM SDK，调用这个接口后，可以立即调用登录接口
        V2TIMManager.getInstance().initSDK(this, Integer.parseInt("1600013134"), config);
    }

    /**
     * 设置按钮点击事件监听器
     */
    private void setupButtonListeners() {
        // 登录按钮点击事件
        loginButton.setOnClickListener(v -> {
            String token = tokenInput.getText().toString().trim();
            if (TextUtils.isEmpty(token)) {
                updateStatus("请输入Token");
                return;
            }
            updateStatus("正在登录...");
            loginWithToken(token);
        });

        // 发送文本消息按钮点击事件
        sendButton.setOnClickListener(v -> {
            String targetUserID = targetUserIDInput.getText().toString().trim();
            String message = messageInput.getText().toString().trim();
            if (TextUtils.isEmpty(targetUserID)) {
                updateStatus("请输入目标用户ID");
                return;
            }
            if (TextUtils.isEmpty(message)) {
                updateStatus("请输入消息内容");
                return;
            }
            updateStatus("正在发送文本消息...");
            sendCustomMessage(targetUserID, message);
        });

        // 选择图片按钮点击事件
        selectImageButton.setOnClickListener(v -> {
            // 打开文件选择器选择图片
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            imagePickerLauncher.launch(intent);
        });

        // 发送图片消息按钮点击事件
        sendImageButton.setOnClickListener(v -> {
            String targetUserID = targetUserIDInput.getText().toString().trim();
            String imagePath = imagePathInput.getText().toString().trim();
            if (TextUtils.isEmpty(targetUserID)) {
                updateStatus("请输入目标用户ID");
                return;
            }
            if (TextUtils.isEmpty(imagePath)) {
                updateStatus("请选择图片");
                return;
            }
            updateStatus("正在发送图片消息...");
            sendImageMessage(targetUserID, imagePath);
        });
    }


    /**
     * 使用Token登录
     * @param token 用户Token
     */
    private void loginWithToken(String token) {
        new Thread(() -> {
            try {
                Map<String, String> result = HttpRequestUtil.sendUserinfoRequest(token);
                String userID = result.get("id");
                String userSig = result.get("userSig");

                // 切换到主线程显示结果
                runOnUiThread(() -> {
                    Log.i("imsdk", "获取到的 UserID: " + userID);
                    Log.i("imsdk", "获取到的 UserSig: " + userSig);
                    updateStatus("获取用户信息成功，正在登录...");
                });

                // 在新线程中执行登录（IM SDK登录操作）
                new Thread(() -> V2TIMManager.getInstance().login(userID, userSig, new V2TIMCallback() {
                    @Override
                    public void onSuccess() {
                        Log.i("imsdk", "IM登录成功");
                        // 保存Token到SharedPreferences
                        getSharedPreferences("user_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("user_token", token)
                                .apply();
                        
                        // 登录成功后再切换到主线程
                        runOnUiThread(() -> {
                            updateStatus("登录成功  UserID: " + userID);
                            Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(int code, String desc) {
                        Log.e("imsdk", "IM登录失败, code:" + code + ", desc:" + desc);
                        // 切换到主线程显示错误
                        runOnUiThread(() -> {
                            updateStatus("登录失败: " + desc);
                            Toast.makeText(MainActivity.this, "登录失败：" + desc, Toast.LENGTH_SHORT).show();
                        });
                    }
                })).start();

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Log.e("imsdk", "请求失败: " + e.getMessage());
                    updateStatus("请求失败: " + e.getMessage());
                });
            } catch (Exception e) {
                Log.e("imsdk", "异常: " + e.getMessage());
                // 检查是否为Token被顶下线错误
                if (e.getMessage() != null && e.getMessage().contains("Token已被顶下线")) {
                    // 清除保存的Token
                    getSharedPreferences("user_prefs", MODE_PRIVATE)
                            .edit()
                            .remove("user_token")
                            .apply();
                    runOnUiThread(() -> {
                        updateStatus("Token已被顶下线，请重新登录");
                        Toast.makeText(MainActivity.this, "Token已被顶下线，请重新登录", Toast.LENGTH_LONG).show();
                        // 清空Token输入框
                        tokenInput.setText("");
                    });
                } else {
                    runOnUiThread(() -> updateStatus("异常: " + e.getMessage()));
                }
            }
        }).start();
    }

    /**
     * 发送自定义文本消息
     * @param targetUserID 目标用户ID
     * @param messageText 消息文本
     */
    private void sendCustomMessage(String targetUserID, String messageText) {
        V2TIMManager.getInstance().sendC2CTextMessage(messageText, targetUserID, new V2TIMValueCallback<>() {
            @Override
            public void onSuccess(V2TIMMessage message) {
                Log.i("imsdk", "发送单聊文本消息成功, msgID: " + message.getMsgID());
                // 切换到主线程更新UI
                runOnUiThread(() -> {
                    updateStatus("文本消息发送成功");

                });
            }

            @Override
            public void onError(int code, String desc) {
                Log.e("imsdk", "发送消息失败, code: " + code + ", desc: " + desc);
                runOnUiThread(() -> updateStatus("发送消息失败, code: " + code + ", desc: " + desc));
            }
        });
    }

    /**
     * 发送图片消息
     * @param targetUserID 目标用户ID
     * @param imagePath 图片文件路径
     */
    private void sendImageMessage(String targetUserID, String imagePath) {
        // 检查文件是否存在
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            runOnUiThread(() -> updateStatus("图片文件不存在"));
            return;
        }
        
        // 检查文件大小（最大28MB）
        long fileSize = imageFile.length();
        long maxSize = 28 * 1024 * 1024; // 28MB
        if (fileSize > maxSize) {
            runOnUiThread(() -> updateStatus("图片文件过大，最大支持28MB"));
            return;
        }

        // 创建图片消息
        V2TIMMessage message = V2TIMManager.getMessageManager().createImageMessage(imagePath);
        
        // 发送图片消息
        V2TIMManager.getMessageManager().sendMessage(message, targetUserID, null, V2TIMMessage.V2TIM_PRIORITY_NORMAL, false, null, new V2TIMSendCallback<V2TIMMessage>() {
            @Override
            public void onProgress(int progress) {
                // 发送进度
                runOnUiThread(() -> updateStatus("发送进度: " + progress + "%"));
            }

            @Override
            public void onSuccess(V2TIMMessage v2TIMMessage) {
                // 发送成功
                runOnUiThread(() -> {
                    updateStatus("图片消息发送成功"+v2TIMMessage.getMsgID());
                });
            }

            @Override
            public void onError(int code, String desc) {
                // 发送失败
                runOnUiThread(() -> updateStatus("图片消息发送失败: " + desc));
            }
        });
    }

    /**
     * 更新状态显示
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        statusText.setText(status);
        Log.i("imsdk", "状态更新: " + status);
    }

    // 图片选择器的ActivityResultLauncher
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // 获取文件路径
                        String filePath = getFilePathFromUri(uri);
                        if (filePath != null) {
                            imagePathInput.setText(filePath);
                            updateStatus("已选择图片: " + filePath);
                        } else {
                            updateStatus("无法获取图片路径");
                        }
                    }
                } else {
                    updateStatus("取消选择图片");
                }
            }
    );

    /**
     * 从Uri获取文件路径
     * @param uri 文件Uri
     * @return 文件路径，如果无法获取则返回null
     */
    private String getFilePathFromUri(Uri uri) {
        String filePath = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (displayNameIndex != -1) {
                        String fileName = cursor.getString(displayNameIndex);
                        // 创建临时文件路径
                        File tempFile = new File(getCacheDir(), fileName);
                        try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                             java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            filePath = tempFile.getAbsolutePath();
                        } catch (IOException e) {
                            Log.e("imsdk", "复制文件失败: " + e.getMessage());
                        }
                    }
                }
            }
        } else if (uri.getScheme().equals("file")) {
            filePath = uri.getPath();
        }
        return filePath;
    }
}