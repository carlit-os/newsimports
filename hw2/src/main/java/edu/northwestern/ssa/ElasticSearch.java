package edu.northwestern.ssa;

import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class ElasticSearch extends AwsSignedRestRequest{


    /**
     * @param serviceName would be "es" for Elasticsearch
     */
    public ElasticSearch(String serviceName) {
        super(serviceName);
    }





    public AbortableInputStream getDoc(String host , String idx, Optional<Map<String,String>> dict) throws IOException {

        HttpExecuteResponse getit = this.restRequest(SdkHttpMethod.GET,host, idx, dict);

        return getit.responseBody().get();

    }




}
