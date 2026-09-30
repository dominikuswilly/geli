package com.shop.warehouse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shop Warehouse Inventory Management API")
                        .version("1.0.0")
                        .description("Sistem RESTful API backend untuk manajemen gudang toko retail, pelacakan varian produk, penetapan harga berjenjang, dan proteksi anti-overselling.")
                        .contact(new Contact()
                                .name("Fullstack Development Team")
                                .email("dev@shop-warehouse.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
