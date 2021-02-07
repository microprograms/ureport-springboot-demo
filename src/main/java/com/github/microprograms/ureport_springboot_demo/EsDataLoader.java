package com.github.microprograms.ureport_springboot_demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.microprograms.ureport_springboot_demo.utils.JsonUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
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

    public JSONObject getDatasetConfig(JSONObject dsConfig, String datasetName) {
        return dsConfig.getJSONObject("datasets").getJSONObject(datasetName);
    }

    public List<?> loadReportData(String dsName, String datasetName, Map<String, Object> parameters)
            throws IOException {
        JSONObject dsConfig = getDsConfig(dsName);
        String url = dsConfig.getString("url");
        String source = dsConfig.getJSONObject("search").toJSONString();
        String _responseString = search(url, source);
        JSONObject responseJsonObject = JSON.parseObject(_responseString);

        JSONObject datasetConfig = getDatasetConfig(dsConfig, datasetName);
        String path = datasetConfig.getString("path").replaceAll("\\s*", "");
        PostprocessCmds postprocessCmds = new PostprocessCmds(datasetConfig.getJSONArray("postprocess"));
        postprocessCmds.execute(responseJsonObject, responseJsonObject);

        return JsonUtils.getJsonArray(responseJsonObject, path);

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

    public static class PostprocessCmds {
        private List<PostprocessCmd> list = new ArrayList<>();

        public PostprocessCmds(JSONArray cmds) {
            for (int i = 0; i < cmds.size(); i++) {
                PostprocessCmd cmd = new PostprocessCmd((String) cmds.get(i));
                if (cmd.isEmptyCmd()) {
                    // 忽略空命令
                    continue;
                }
                list.add(cmd);
            }
        }

        public void execute(JSONObject root, Object current) {
            if (list.isEmpty()) {
                return;
            }

            list.remove(0)._execute(root, current);
        }

        public boolean isEmpty() {
            return list.isEmpty();
        }

        public class PostprocessCmd {
            private String cmd;
            private String[] args;

            public PostprocessCmd(String rawCmd) {
                if (StringUtils.isBlank(rawCmd)) {
                    return;
                }

                String[] split = rawCmd.trim().split("\\s+");
                cmd = split[0];

                if (split.length > 1) {
                    args = Arrays.copyOfRange(split, 1, split.length);
                }
            }

            public void _execute(JSONObject root, Object current) {
                switch (getCmd()) {
                    case "cd":
                        _cd(root, current);
                        break;
                    case "copy":
                        _copy(root, current);
                        break;
                }
            }

            private void _cd(JSONObject root, Object current) {
                if (isEmptyArgs()) { // 默认转到根对象
                    execute(root, root);
                }

                String[] args = getArgs();
                String path = args[0];
                if (path.endsWith("[*]")) {
                    execute(root, JsonUtils.getJsonArray(root, path));
                } else {
                    execute(root, JsonUtils.getJsonObject(root, path));
                }
            }

            private void _copy(JSONObject root, Object current) {
                if (getArgsCount() != 2) {
                    throw new RuntimeException("args count must = 2");
                }

                if (current instanceof JSONObject) {
                    _copyForJsonObject(root, (JSONObject) current);
                }

                if (current instanceof JSONArray) {
                    JSONArray currentJsonArray = (JSONArray) current;
                    for (int i = 0; i < currentJsonArray.size(); i++) {
                        _copyForJsonObject(root, currentJsonArray.getJSONObject(i));
                    }
                }

                execute(root, current);
            }

            private void _copyForJsonObject(JSONObject root, JSONObject currentJsonObject) {
                String from = getArg(0);
                String to = getArg(1);
                JsonUtils.set(currentJsonObject, to, JsonUtils.select(currentJsonObject, from));
            }

            public String getCmd() {
                return cmd;
            }

            public String[] getArgs() {
                return args;
            }

            public int getArgsCount() {
                return args.length;
            }

            public String getArg(int index) {
                return args[index];
            }

            public boolean isEmptyCmd() {
                return null == cmd;
            }

            public boolean isEmptyArgs() {
                return args.length == 0;
            }
        }
    }
}
