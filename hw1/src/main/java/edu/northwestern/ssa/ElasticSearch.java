package edu.northwestern.ssa;

import netscape.javascript.JSObject;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.io.IOException;
import java.util.Optional;


public class ElasticSearch extends AwsSignedRestRequest {
    private static final String ELASTIC_SEARCH_HOST = System.getenv("ELASTIC_SEARCH_HOST");
    private static final String ELASTIC_SEARCH_INDEX = System.getenv("ELASTIC_SEARCH_INDEX");

    /**
     * @param serviceName would be "es" for Elasticsearch
     */
    ElasticSearch(String serviceName) {
        super(serviceName);
    }


    //create index method
    public void createIndex() throws IOException {
        restRequest(SdkHttpMethod.PUT,ELASTIC_SEARCH_HOST,ELASTIC_SEARCH_INDEX, Optional.empty());
    }

    //delete index method
    public void deleteIndex() throws IOException {
        restRequest(SdkHttpMethod.DELETE,ELASTIC_SEARCH_HOST,ELASTIC_SEARCH_INDEX, Optional.empty());
    }



    //postdoc method
    public void postDoc(JSObject jGoodies){
        String postHost = ELASTIC_SEARCH_HOST + "/_doc/";

        restRequest(SdkHttpMethod.POST,ELASTIC_SEARCH_HOST,postHost,);
    }


}
