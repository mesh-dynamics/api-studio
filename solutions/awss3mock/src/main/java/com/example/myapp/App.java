/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.myapp;

import java.io.IOException;
import java.net.URI;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class App {

    public static void main(String[] args) throws IOException {

        SdkHttpClient apacheClientFactory =
            ApacheHttpClient.builder()
                .proxyConfiguration(ProxyConfiguration.builder()
                    .endpoint(URI.create("http://127.0.0.1:9002")).build()).build();


        Region region = Region.US_WEST_2;
        S3Client s3 = S3Client.builder()
            .httpClient(apacheClientFactory).region(region).build();

        //S3Client s3 = S3Client.builder().region(region).build();

        String bucket = "bucketmdtest";
        String key = "key";

        System.out.println("Uploading object...");

        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                        .build(),
                RequestBody.fromString("Testing with the AWS SDK for Java"));

        System.out.println("Upload complete");
        System.out.printf("%n");

        System.out.println("Closing the connection to Amazon S3");
        s3.close();
        System.out.println("Connection closed");
        System.out.println("Exiting...");
    }
}
