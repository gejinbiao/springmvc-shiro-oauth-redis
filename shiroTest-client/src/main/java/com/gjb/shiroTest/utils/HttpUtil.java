package com.gjb.shiroTest.utils;

import org.apache.commons.collections.map.HashedMap;

import java.util.Map;

/**
 * @author gejinbiao@ucfgroup.com
 * @Title
 * @Description:
 * @Company: ucfgroup.com
 * @Created 2018-01-09
 */
public class HttpUtil {

    public static void main(String[] args){
        try{
            Map<String,Object> params = new HashedMap();
            params.put("code","123");
            params.put("client_id","c1ebe466-1cdc-4bd3-ab69-77c3561b9dee");
            params.put("client_secret","d8346ea2-6017-43ed-ad68-19c0f971738b");
            params.put("grant_type","authorization_code");
            params.put("redirect_uri","http://localhost:8081/test/page");

            String url ="http://localhost:8080/accessToken";
            String post = com.xiaoleilu.hutool.http.HttpUtil.post(url, params);
            System.out.println(post);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }
}
