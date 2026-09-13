package com.aimi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.aimi.mapper")
public class QauAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(QauAppApplication.class, args);
    }
}
