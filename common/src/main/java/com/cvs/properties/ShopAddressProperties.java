package com.cvs.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cvs.shop")
@Data
public class ShopAddressProperties {
    private String address;
}
