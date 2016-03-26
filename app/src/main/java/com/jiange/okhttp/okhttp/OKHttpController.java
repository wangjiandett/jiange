/*
 * Copyright (C) 20015 MaiNaEr All rights reserved
 */
package com.jiange.okhttp.okhttp;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.ieclipse.af.volley.IUrl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * okhttp加载基类，子类需继承之实现自定义功能
 *
 * @author wangjian
 * @date 2016/3/23.
 */
public abstract class OKHttpController<Listener> {

    public static final int SUCCESS_CODE = 0x01;
    public static final int ERROR_CODE = 0x02;
    protected Listener mListener;
    private Call mCall = null;

    public OKHttpController() {
    }

    public OKHttpController(Listener l) {
        super();
        setListener(l);
    }

    public void setListener(Listener l) {
        this.mListener = l;
    }

    private void log(String msg) {
        android.util.Log.e(OKHttpController.class.getSimpleName(), msg);
    }

    protected abstract class LoadTask<Input, Output> implements Callback {

        private OkHttpClient mClient = OKHttpManager.getOkHttpClient();
        private MediaType MEDIA_TYPE_PLAIN = MediaType.parse("text/plain;charset=utf-8");
        protected Class<Output> mDataClazz;
        protected Class<?> mDataItemClass;
        private Gson mGson = new Gson();
        protected Input input;

        protected abstract IUrl getUrl();

        protected abstract void onSuccess(Output output);

        protected abstract void onError(String error);

        /**
         * Intercept 'data' json parser
         *
         * @param response the whole response object ({@link OKBaseResponse} instance)
         * @return true if you want to skip convert 'data' json to object.
         */
        protected boolean onInterceptor(OKBaseResponse response) {
            return false;
        }

        public void load2List(Input input, Class<?> itemClass, boolean needCache) {
            this.mDataItemClass = itemClass;
            load(input, null);
        }

        public void load(Input input, Class<Output> clazz) {
            this.input = input;
            this.mDataClazz = clazz;

            IUrl url = getUrl();
            log("request url = " + url.getUrl());
            // 获取请求方法
            int method = url.getMethod();
            Request.Builder builder = null;
            RequestBody requestBody = null;
            builder = new Request.Builder();
            switch (method) {
                case Method.GET:
                    // 获取请求体
                    bindBody(url, builder).get();
                    break;
                case Method.POST:
                    requestBody = postBody(input);
                    builder.url(url.getUrl()).post(requestBody);
                    break;
                case Method.DELETE:
                    bindBody(url, builder).delete();
                    break;
                case Method.PUT:
                    requestBody = RequestBody.create(MEDIA_TYPE_PLAIN, getBody(input));
                    bindBody(url, builder).put(requestBody);
                    break;
                case Method.HEAD:
                    bindBody(url, builder).head();
                    break;
                case Method.PATCH:
                    requestBody = RequestBody.create(MEDIA_TYPE_PLAIN, getBody(input));
                    builder.url(url.getUrl()).patch(requestBody);
                    break;
            }

            // 构建tag
            String tag = getClass().getName();
            // 封装请求.cacheControl(new CacheControl.Builder().maxAge(5, TimeUnit.SECONDS).build())
            Request request = builder.tag(tag).build();
            // 执行请求
            mCall = mClient.newCall(request);
            mCall.enqueue(this);
        }

        private Request.Builder bindBody(IUrl url, Request.Builder builder) {
            String body = getBody(input);
            url.setQuery(body);
            builder.url(url.getUrl());
            log("get request url = " + url.getUrl());
            return builder;
        }

        @Override
        public final void onFailure(Call call, IOException e) {
            sendMessage(e.getMessage(), ERROR_CODE);
        }

        @Override
        public final void onResponse(Call call, Response response) {
            if (response != null && response.isSuccessful() && response.code() == 200) {
                convertData(response);
            }
            else {
                throw new NullPointerException("base response is null, please check your http response.");
            }
        }

        private void convertData(Response response) {
            Output out = null;
            String body = null;
            try {
                body = response.body().string().trim();
                // 解析成OKBaseResponse
                OKBaseResponse baseResponse = mGson.fromJson(body, getBaseResponseClass());
                if (!onInterceptor(baseResponse)) {
                    // mDataClazz是否是BaseResponse
                    if (mDataClazz != null) {
                        boolean baseOutput = isOKBaseResponse(mDataClazz);
                        if (baseOutput) {
                            out = (Output) baseResponse;
                            sendMessage(out, SUCCESS_CODE);
                            return;
                        }
                    }
                    // 解析成BaseResponse中的data
                    String data = baseResponse.getData();
                    if (!TextUtils.isEmpty(data)) {
                        log("response = " + body);
                        if (mDataItemClass != null && mDataClazz == null) {
                            out = mGson.fromJson(data, type(List.class, mDataItemClass));
                            if (out == null) {
                                out = (Output) new ArrayList<>(0);
                            }
                        }
                        else {
                            out = mGson.fromJson(data, mDataClazz);
                        }
                        sendMessage(out, SUCCESS_CODE);
                    }
                }
            } catch (IOException e) {
                sendMessage(e.getMessage(), ERROR_CODE);
                return;
            } catch (JsonSyntaxException e) {
                sendMessage("数据解析失败", ERROR_CODE);
                return;
            } catch (Exception e) {
                sendMessage(e.getMessage(), ERROR_CODE);
                return;
            }
        }

        protected void sendMessage(Object obj, int what) {
            Message msg = Message.obtain();
            msg.obj = obj;
            msg.what = what;
            handler.sendMessage(msg);
        }

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == SUCCESS_CODE) {
                    onSuccess((Output) msg.obj);
                }
                else if (msg.what == ERROR_CODE) {
                    onError(msg.obj.toString());
                }
            }
        };

        /**
         * Get parameter encoding
         *
         * @return request parameter encoding
         * @see com.android.volley.Request#getParamsEncoding
         */

        public String getParamsEncoding() {
            return "UTF-8";
        }

        protected RequestBody postBody(Input input) {
            HashMap<String, ?> map = OkStringUtils.postRequestParam(input, getParamsEncoding());
            FormBody.Builder formBuilder = new FormBody.Builder();
            if (map != null) {
                StringBuffer buffer = new StringBuffer();

                for (String key : map.keySet()) {
                    buffer.append(key).append("=").append(map.get(key)).append(" ,");
                    formBuilder.add(key, OkStringUtils.getRequestParamValue(map.get(key), getParamsEncoding()));
                }
                log("request body: " + buffer.deleteCharAt(buffer.toString().length() - 1));
            }
            return formBuilder.build();
        }

        /**
         * Get request body.
         *
         * @param input request entity
         * @return encoded body string
         * @see #getParamsEncoding()
         */
        protected String getBody(Input input) {
            String body = null;
            if (input == null) {
                body = null;
            }
            else if (input instanceof Map) {
                Map map = (Map) input;
                StringBuilder sb = new StringBuilder();
                for (Object key : map.keySet()) {
                    sb.append(key);
                    sb.append('=');
                    sb.append(OkStringUtils.getRequestParamValue(map.get(key), getParamsEncoding()));
                    sb.append('&');
                }
                if (sb.length() > 1) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                body = sb.toString();
            }
            else {
                body = OkStringUtils.getRequestParam(input, getParamsEncoding());
            }
            log("request body: " + body);
            return body;
        }
    }

    public void onDestroy() {
        if (mCall != null && !mCall.isExecuted()) {
            try {
                mCall.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Class<? extends OKBaseResponse> getBaseResponseClass() {
        return OKHttpManager.getConfig().getBaseResponseClass();
    }

    private static boolean isOKBaseResponse(Class<?> clazz) {
        List<Class<?>> list = getSuperType(clazz);
        return list.contains(OKBaseResponse.class);
    }

    public static List<Class<?>> getSuperType(Class<?> clazz) {
        List<Class<?>> set = new ArrayList<>();
        getSuperType(clazz, set);
        return set;
    }

    private static void getSuperType(Class<?> clazz, List<Class<?>> set) {
        set.add(clazz);
        if (clazz.getSuperclass() != null) {
            getSuperType(clazz.getSuperclass(), set);
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> c : interfaces) {
            getSuperType(c, set);
        }
    }

    public static ParameterizedType type(final Class raw, final Type... args) {
        return new ParameterizedType() {
            public Type getRawType() {
                return raw;
            }

            public Type[] getActualTypeArguments() {
                return args;
            }

            public Type getOwnerType() {
                return null;
            }
        };
    }

    public interface Method {
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int PATCH = 5;
    }
}
