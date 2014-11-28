package com.example.kino.zukan;

import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.CookieStore;
import java.util.ArrayList;

/**
 * Created by p on 2014/11/24.
 */
public class PostMessageTask extends AsyncTask<String, Integer, Integer> {
    DefaultHttpClient httpClient;
    public PostMessageTask(DefaultHttpClient client){
        httpClient = client;
    }
    @Override
    protected Integer doInBackground(String... contents) {



        String url="http://zukan.com/api/v0/get_search_items_internal.json";

        HttpPost post = new HttpPost(url);

        ArrayList<NameValuePair> params = new ArrayList <NameValuePair>();
        params.add( new BasicNameValuePair("key", contents[0]));
        params.add(new BasicNameValuePair("zukan_alias",contents[1]));


        HttpResponse res = null;

        try {
            post.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
            post.setHeader("Referer","http://zukan.com/");
            post.setHeader("Content-Type","application/json");
            post.setHeader("Origin","http://zukan.com");
            res = httpClient.execute(post);

        } catch (IOException e) {
            e.printStackTrace();
        }

        int status = res.getStatusLine().getStatusCode();
        String ReceiveStr = new String();

        try{
        if (status != HttpStatus.SC_OK) {
            try {
                throw new Exception("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ReceiveStr = EntityUtils.toString(res.getEntity(), "UTF-8");
        }}catch (IOException e) {
            e.printStackTrace();
        }

        return Integer.valueOf(res.getStatusLine().getStatusCode());
    }

}