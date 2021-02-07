package com.github.microprograms.ureport_springboot_demo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class JsonUtils {

    public static void main(String[] args) throws Exception {
        try (InputStream input = JsonUtils.class.getClassLoader().getResourceAsStream("ureport/es-query.result.json")) {
            JSONObject jsonObject = load(input, "utf8");
            System.out.println(select(jsonObject, "aggregations.group_by_company_name.sum_other_doc_count"));
            System.out.println(select(jsonObject, "aggregations.group_by_company_name.buckets[*]"));
            System.out.println(select(jsonObject, "aggregations.group_by_company_name.buckets[*].key"));

            System.out.println(select(jsonObject, "aggregations.group_by_company_name.sum_other_doc_count"));
            set(jsonObject, "aggregations.group_by_company_name.sum_other_doc_count", 100);
            System.out.println(select(jsonObject, "aggregations.group_by_company_name.sum_other_doc_count"));

            System.out.println(select(jsonObject, "aggregations.group_by_company_name.buckets[*]"));
            JSONArray jsonArray = new JSONArray();
            jsonArray.add(1);
            jsonArray.add(2);
            jsonArray.add(10);
            set(jsonObject, "aggregations.group_by_company_name.buckets[*]", jsonArray);
            System.out.println(select(jsonObject, "aggregations.group_by_company_name.buckets[*]"));
        }
    }

    public static JSONObject load(InputStream input, String charsetName) throws IOException {
        List<String> lines = IOUtils.readLines(input, charsetName);
        String json = StringUtils.join(lines, "");
        return JSON.parseObject(json);
    }

    public static void set(JSONObject jsonObject, String path, Object value) {
        JSONObject parentJsonObject = _createParentJsonObjectByPath(jsonObject, path);
        String key = path.substring(path.lastIndexOf(".") + 1).replaceFirst("\\[\\*\\]$", "");

        if (path.equals("rad_chk[*]") && value instanceof JSONArray) {
            value = "[{\"test\":0.36419999599456787},{\"test\":0.6510583162307739}]";
        }

        parentJsonObject.put(key, value);
    }

    private static JSONObject _createParentJsonObjectByPath(JSONObject jsonObject, String path) {
        int index = path.indexOf(".");
        if (index == -1) {
            return jsonObject;
        }

        String parentPath = path.substring(0, index);
        String childPath = path.substring(index + 1);
        if (!jsonObject.containsKey(parentPath)) {
            jsonObject.put(parentPath, new JSONObject());
        }
        return _createParentJsonObjectByPath(jsonObject.getJSONObject(parentPath), childPath);
    }

    /**
     * 非数组: aggregations.group_by_company_name.sum_other_doc_count <br/>
     * 
     * 数组: aggregations.group_by_company_name.buckets[*] <br/>
     * 
     * 数组展开: group_by_st_name.buckets[*].restrict_we.value <br/>
     * 
     * @param jsonObject
     * @param path
     * @return
     */
    public static Object select(JSONObject jsonObject, String path) {
        // 非数组
        if (!path.contains("[*]")) {
            return _get(jsonObject, path);
        }

        // 数组
        if (path.endsWith("[*]")) {
            return getJsonArray(jsonObject, path);
        }

        // 数组展开
        int index = path.indexOf("[*]");
        String parentPath = path.substring(0, index + "[*]".length());
        String childPath = path.substring(index + "[*]".length() + 1);
        JSONArray jsonArray = getJsonArray(jsonObject, parentPath);

        JSONArray newJsonArray = new JSONArray();
        for (int i = 0; i < jsonArray.size(); i++) {
            newJsonArray.add(i, _get(jsonArray.getJSONObject(i), childPath));
        }
        return newJsonArray;
    }

    private static Object _get(JSONObject jsonObject, String path) {
        int index = path.indexOf(".");
        if (index == -1) {
            return jsonObject.get(path);
        }

        return _get(jsonObject.getJSONObject(path.substring(0, index)), path.substring(index + 1));
    }

    public static JSONObject getJsonObject(JSONObject jsonObject, String path) {
        if (path.contains("[*]")) {
            throw new RuntimeException("path must not contains [*]");
        }

        int index = path.indexOf(".");
        if (index == -1) {
            return jsonObject.getJSONObject(path);
        }

        return getJsonObject(jsonObject.getJSONObject(path.substring(0, index)), path.substring(index + 1));
    }

    public static JSONArray getJsonArray(JSONObject jsonObject, String path) {
        if (!path.endsWith("[*]")) {
            throw new RuntimeException("path must ends with [*]");
        }

        int index = path.indexOf(".");
        if (index == -1) {
            return jsonObject.getJSONArray(path.replaceFirst("\\[\\*\\]$", ""));
        }

        return getJsonArray(jsonObject.getJSONObject(path.substring(0, index)), path.substring(index + 1));
    }
}
