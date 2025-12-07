package com.flight.airchat;

import android.util.Log;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class HttpRequestUtil {

    /**
     * 发送 POST 请求
     * @param urlStr 请求URL
     * @param headers 请求头
     * @param body 请求体（JSON格式）
     * @return 响应内容
     */
    public static String sendPost(String urlStr, Map<String, String> headers, String body) throws IOException {
        return sendRequest("POST", urlStr, headers, body);
    }

    /**
     * 发送 GET 请求
     * @param urlStr 请求URL
     * @param headers 请求头
     * @return 响应内容
     */
    public static String sendGet(String urlStr, Map<String, String> headers) throws IOException {
        return sendRequest("GET", urlStr, headers, null);
    }

    /**
     * 发送 HTTP 请求
     * @param method 请求方法（GET/POST）
     * @param urlStr 请求URL
     * @param headers 请求头
     * @param body 请求体
     * @return 响应内容
     */
    private static String sendRequest(String method, String urlStr, Map<String, String> headers, String body) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod(method.toUpperCase());

        // 设置请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpConn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        // 对于 POST 请求，设置请求体
        if ("POST".equalsIgnoreCase(method) && body != null) {
            httpConn.setDoOutput(true);
            try (OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream())) {
                writer.write(body);
                writer.flush();
            }
        }

        // 获取响应
        int responseCode = httpConn.getResponseCode();
        InputStream responseStream = responseCode / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();

        // 处理 gzip 压缩
        if ("gzip".equals(httpConn.getContentEncoding())) {
            responseStream = new GZIPInputStream(responseStream);
        }

        // 读取响应内容
        try (Scanner scanner = new Scanner(responseStream).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }


    private static @NotNull String getNo() {
        BigInteger valueOf = BigInteger.valueOf(Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).getTimeInMillis());
        BigInteger valueOf2 = BigInteger.valueOf(0x039302fb);
        return valueOf.xor(valueOf2).shiftLeft(7).toString();
    }

    public static Map<String, String> sendUserinfoRequest(String token) throws Exception {
        String url = "https://api.airchat.cc/client/biz/userinfo/detail";
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Host", "api.airchat.cc");
        headers.put("token", token);
        headers.put("x-request-no", getNo());
        headers.put("content-type", "application/json; charset=UTF-8");
        headers.put("accept", "application/json; charset=UTF-8");
        headers.put("content-length", "0");
        headers.put("cookie", token);
        headers.put("user-agent", "okhttp/4.11.0");

        String json = sendGet(url, headers);

        // 先打印完整的响应，用于调试
        Log.d("imsdk", "API响应: " + json);

        JSONObject jsonObject = new JSONObject(json);

        // 检查响应码
        int code = jsonObject.optInt("code", -1);
        String msg = jsonObject.optString("msg", "");

        Log.d("imsdk", "响应码: " + code + ", 消息: " + msg);

        // 安全地获取data字段
        if (!jsonObject.has("data") || jsonObject.isNull("data")) {
            Log.e("imsdk", "data字段为空或不存在");
            throw new Exception("API返回数据异常，data字段为空");
        }

        JSONObject dataObject = jsonObject.getJSONObject("data");

        // 提取id和userSig
        String id = dataObject.optString("id", "");
        String userSig = dataObject.optString("userSig", "");

        if (id.isEmpty() || userSig.isEmpty()) {
            Log.e("imsdk", "id或userSig为空");
            Log.e("imsdk", "id: " + id + ", userSig: " + userSig);
        }

        Map<String, String> result = new HashMap<>();
        result.put("id", id);
        result.put("userSig", userSig);

        return result;
    }
}