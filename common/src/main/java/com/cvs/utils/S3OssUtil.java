package com.cvs.utils;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

@Data
@AllArgsConstructor
@Slf4j
public class S3OssUtil {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String objectPath;

    public String upload(byte[] bytes,String objName){

            Region region = Region.of("us-west-004");
            AwsSessionCredentials credentials = AwsSessionCredentials.create(accessKeyId, accessKeySecret, "");
            S3Client s3Client = S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(credentials)).endpointOverride(URI.create(endpoint)).region(region).build();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectPath + objName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));

            GetUrlRequest getUrlRequest = GetUrlRequest.builder().bucket(bucketName)
                    .key(objectPath + objName)
                    .build();
            URL url = s3Client.utilities().getUrl(getUrlRequest);
            return url.toString();
        } catch (S3Exception ex){
            System.err.println(ex.getMessage());
        } finally {
            s3Client.close();
        }
        return null;
    }
}
