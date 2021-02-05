package com.github.microprograms.ureport_springboot_demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class EsDataLoader {

    private JSONObject config;

    public EsDataLoader(InputStream configInputStream) {
        try {
            List<String> lines = IOUtils.readLines(configInputStream, "utf8");
            String json = StringUtils.join(lines, "");
            config = JSON.parseObject(json);
        } catch (IOException e) {
            // error
        }
    }

    public JSONObject getDsConfig(String dsName) {
        return config.getJSONObject("datasources").getJSONObject(dsName);
    }

    public List<?> loadReportData(String dsName, String datasetName, Map<String, Object> parameters)
            throws IOException {
        JSONObject dsConfig = getDsConfig(dsName);
        String url = dsConfig.getString("url");
        String source = dsConfig.getJSONObject("search").toJSONString();
        String path = dsConfig.getJSONObject("datasets").getJSONObject(datasetName).getString("path");

        String response = search(url, source);
        JSONObject jsonObject = JSON.parseObject(response);
        JSONArray jsonArray = getJsonArray(jsonObject, path);
        return jsonArray;

        // List<Map<String, String>> list = new ArrayList<>();
        // for (int i = 0; i < jsonArray.size(); i++) {
        // JSONObject x = jsonArray.getJSONObject(i);
        // Map<String, String> map = new HashMap<>();
        // for (String key : x.keySet()) {
        // map.put(key, x.getString(key));
        // }
        // list.add(map);
        // }

        // return list;
    }

    public static JSONArray getJsonArray(JSONObject jsonObject, String path) {
        int index = path.indexOf(".");
        if (index == -1) {
            return jsonObject.getJSONArray(path);
        }
        return getJsonArray(jsonObject.getJSONObject(path.substring(0, index)), path.substring(index + 1));
    }

    public static String search(String url, String source) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost request = new HttpPost(url);
            request.addHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(source, "utf8"));
            HttpResponse httpResponse = httpClient.execute(request);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new IOException("statusCode=" + statusCode);
            }
            return EntityUtils.toString(httpResponse.getEntity(), "utf8");
        }
    }
}
