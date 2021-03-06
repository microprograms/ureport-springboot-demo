package com.github.microprograms.ureport_springboot_demo;

import java.io.InputStream;

import com.bstek.ureport.console.UReportServlet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:ureport-context.xml")
public class UreportDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(UreportDemoApplication.class, args);
	}

	@Bean
	public ServletRegistrationBean<UReportServlet> ureportServlet() {
		return new ServletRegistrationBean<UReportServlet>(new UReportServlet(), "/ureport/*");
	}

	@Bean
	public EsDataLoader esDataLoader() {
		try (InputStream configInputStream = this.getClass().getClassLoader()
				.getResourceAsStream("ureport/es-data-loader.config.json")) {
			return new EsDataLoader(configInputStream);
		} catch (Exception e) {
			return null;
		}
	}

}
