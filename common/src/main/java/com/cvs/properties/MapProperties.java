package com.cvs.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cvs.map")
@Data
public class MapProperties {
    private String ak;
    private String sk;
    private String apiDomain;
    private String locateApiUri;
    private String directApiUri;
}
