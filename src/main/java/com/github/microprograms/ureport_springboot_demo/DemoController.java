package com.github.microprograms.ureport_springboot_demo;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.bstek.ureport.export.ExportConfigureImpl;
import com.bstek.ureport.export.ExportManager;

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
}
