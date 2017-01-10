package com.orange.oss.prometheus.iaasexporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.prometheus.iaasexporter.cloudstack.CloudStackScan;
import com.orange.oss.prometheus.iaasexporter.openstack.OpenStackScan;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;

@Configuration
@ConditionalOnClass(CollectorRegistry.class)
public class PrometheusConfiguration {

	private static Logger logger=LoggerFactory.getLogger(PrometheusConfiguration.class.getName());
	
	
     @Bean
     @ConditionalOnMissingBean
     CollectorRegistry metricRegistry() {
         return CollectorRegistry.defaultRegistry;
     }

     @Bean
     ServletRegistrationBean registerPrometheusExporterServlet(CollectorRegistry metricRegistry) {
           return new ServletRegistrationBean(new MetricsServlet(metricRegistry), "/prometheus");
     }
     
     @Bean
     ExporterRegister exporterRegister() {

           List<Collector> collectors = new ArrayList<>();
           collectors.add(new StandardExports());
           collectors.add(new MemoryPoolsExports());
           ExporterRegister register = new ExporterRegister(collectors);
           return register;
      }     
     
     
     
     
     @Bean
     @ConditionalOnProperty("exporter.openstack.endpoint")     
     OpenStackScan openstack(){
    	 return new OpenStackScan();
     }
     
     @Bean
     @ConditionalOnProperty("exporter.openstack.endpoint")
     NovaApi novaApi(@Value("${exporter.openstack.endpoint}") String endpoint,
                     @Value("${exporter.openstack.tenant}") String tenant,
                     @Value("${exporter.openstack.username}") String username,
                     @Value("${exporter.openstack.password}") String password,
                     @Value("${exporter.proxy_host}") String proxyHost,
                     @Value("${exporter.proxy_port}") String proxyPort    		 
    		 ) {

         String identity = String.format("%s:%s", tenant, username);

         // see https://issues.apache.org/jira/browse/JCLOUDS-816
         Properties overrides = new Properties();
         overrides.put(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
         overrides.put(Constants.PROPERTY_RELAX_HOSTNAME, "true");
         
 		if (proxyHost.length() > 0) {
			logger.info("using proxy {}:{} with user {}", proxyHost,proxyPort);

			overrides.setProperty(Constants.PROPERTY_PROXY_HOST, proxyHost);
			overrides.setProperty(Constants.PROPERTY_PROXY_PORT, proxyPort);
//			overrides.setProperty(Constants.PROPERTY_PROXY_USER, proxy_user);
//			overrides.setProperty(Constants.PROPERTY_PROXY_PASSWORD,proxy_password);
		}

         return ContextBuilder.newBuilder("openstack-nova")
             .endpoint(endpoint)
             .credentials(identity, password)
             .modules(Collections.singleton(new SLF4JLoggingModule()))
             .overrides(overrides)
             .buildApi(NovaApi.class);
     }
     
     @Bean
     @ConditionalOnProperty("exporter.cloudstack.endpoint")
     CloudStackApi cloudstackApi(@Value("${exporter.cloudstack.endpoint}") String endpoint,
                     @Value("${exporter.cloudstack.zone}") String zone,
                     @Value("${exporter.cloudstack.api_key}") String api_key,
                     @Value("${exporter.cloudstack.secret_access_key}") String secret_access_key,
                     @Value("${exporter.proxy_host}") String proxyHost,
                     @Value("${exporter.proxy_port}") String proxyPort    		 
    		 ) {

         String identity = String.format("%s:%s", zone, api_key);

         // see https://issues.apache.org/jira/browse/JCLOUDS-816
         Properties overrides = new Properties();
         overrides.put(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
         overrides.put(Constants.PROPERTY_RELAX_HOSTNAME, "true");
         
 		if (proxyHost.length() > 0) {
			logger.info("using proxy {}:{} with user {}", proxyHost,proxyPort);

			overrides.setProperty(Constants.PROPERTY_PROXY_HOST, proxyHost);
			overrides.setProperty(Constants.PROPERTY_PROXY_PORT, proxyPort);
//			overrides.setProperty(Constants.PROPERTY_PROXY_USER, proxy_user);
//			overrides.setProperty(Constants.PROPERTY_PROXY_PASSWORD,proxy_password);
		}
         
 		
        CloudStackApi api = ContextBuilder.newBuilder("cloudstack")
                .endpoint(endpoint)
                .credentials(api_key, secret_access_key)
                .modules(Collections.singleton(new SLF4JLoggingModule()))
                .overrides(overrides)
                .buildApi(CloudStackApi.class);
        return api;

     }
     
     @Bean
     @ConditionalOnBean(CloudStackApi.class)
     CloudStackScan cloudstackscan(@Value("${exporter.cloudstack.zone}") String zone){
    	 return new CloudStackScan(zone);
     }
     
     
}