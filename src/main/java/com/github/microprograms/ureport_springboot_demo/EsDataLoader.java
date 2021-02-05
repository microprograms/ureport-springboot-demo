package com.github.microprograms.ureport_springboot_demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.builder.SearchSourceBuilder;

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

    public List<Map<String, String>> loadReportData(String dsName, String datasetName, Map<String, Object> parameters)
            throws IOException {
        JSONObject dsConfig = getDsConfig(dsName);
        String[] indices = dsConfig.getJSONArray("indices").toArray(new String[] {});
        String source = dsConfig.getJSONObject("search").toJSONString();
        String path = dsConfig.getJSONObject("datasets").getJSONObject(datasetName).getString("path");

        HttpHost httpHost = new HttpHost("127.0.0.1", 9200, "http");
        String response = search(httpHost, source, indices).toString();
        JSONObject jsonObject = JSON.parseObject(response);
        JSONArray jsonArray = getJsonArray(jsonObject, path);

        List<Map<String, String>> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject x = jsonArray.getJSONObject(i);
            Map<String, String> map = new HashMap<>();
            for (String key : x.keySet()) {
                map.put(key, x.getString(key));
            }
            list.add(map);
        }

        return list;
    }

    public static JSONArray getJsonArray(JSONObject jsonObject, String path) {
        int index = path.indexOf(".");
        if (index == -1) {
            return jsonObject.getJSONArray(path);
        }
        return getJsonArray(jsonObject.getJSONObject(path.substring(0, index)), path.substring(index + 1));
    }

    public static SearchResponse search(HttpHost httpHost, String source, String... indices) throws IOException {
        SearchModule searchModule = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
        XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(
                new NamedXContentRegistry(searchModule.getNamedXContents()),
                DeprecationHandler.THROW_UNSUPPORTED_OPERATION, source);
        SearchRequest searchRequest = new SearchRequest(indices);
        searchRequest.source(SearchSourceBuilder.fromXContent(parser));
        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(httpHost))) {
            return client.search(searchRequest, RequestOptions.DEFAULT);
        }
    }
}
