package com.github.microprograms.ureport_springboot_demo;

import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.bstek.ureport.export.ExportConfigureImpl;
import com.bstek.ureport.export.ExportManager;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/demo")
public class DemoController {

        @Autowired
        private ExportManager exportManager;

        @RequestMapping(value = "/createReport", method = RequestMethod.GET)
        public void createReport() throws Exception {
                Map<String, Object> parameters = new HashMap<String, Object>();
                exportManager.exportPdf(new ExportConfigureImpl("file:test.ureport.xml", parameters,
                                new FileOutputStream("/Users/xuzewei/Downloads/createReport.pdf")));
                exportManager.exportWord(new ExportConfigureImpl("file:test.ureport.xml", parameters,
                                new FileOutputStream("/Users/xuzewei/Downloads/createReport.doc")));
                exportManager.exportExcel(new ExportConfigureImpl("file:test.ureport.xml", parameters,
                                new FileOutputStream("/Users/xuzewei/Downloads/createReport.xlsx")));
        }

        @RequestMapping(value = "/es", method = RequestMethod.GET)
        public void es() throws Exception {
                String indices = "sun_invbox_day";
                String query = "{\"query\":{\"range\":{\"dtime\":{\"gte\":\"2021-02-01\",\"lte\":\"2021-02-01\"}}}}";

                SearchModule searchModule = new SearchModule(Settings.EMPTY, false, Collections.emptyList());
                XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(
                                new NamedXContentRegistry(searchModule.getNamedXContents()),
                                DeprecationHandler.THROW_UNSUPPORTED_OPERATION, query);
                SearchRequest searchRequest = new SearchRequest(indices);
                searchRequest.source(SearchSourceBuilder.fromXContent(parser));
                try (RestHighLevelClient client = new RestHighLevelClient(
                                RestClient.builder(new HttpHost("127.0.0.1", 9200, "http")))) {
                        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                        String response = searchResponse.toString();
                }
        }
}
