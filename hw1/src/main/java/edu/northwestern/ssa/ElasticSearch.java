package edu.northwestern.ssa;

import netscape.javascript.JSObject;
import org.json.HTTP;
import org.json.JSONObject;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
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
        HttpExecuteResponse table = this.restRequest(SdkHttpMethod.PUT,ELASTIC_SEARCH_HOST,ELASTIC_SEARCH_INDEX, Optional.empty());

        table.responseBody().get().close(); //do I need to close this when I create an index or just when postng??
    }

    //delete index method
    public void deleteIndex() throws IOException {
        HttpExecuteResponse erase = this.restRequest(SdkHttpMethod.DELETE,ELASTIC_SEARCH_HOST,ELASTIC_SEARCH_INDEX, Optional.empty());

        erase.responseBody().get().close(); // is this necessary?
    }



    //postdoc method
    public void postDoc(JSONObject jGoodies) throws IOException, InterruptedException {
        String postIdx = ELASTIC_SEARCH_INDEX + "/_doc/";

        Optional<JSONObject> oPjGoodies= Optional.of(jGoodies);


        while(true) {
            try {

                HttpExecuteResponse postit = this.restRequest(SdkHttpMethod.POST, ELASTIC_SEARCH_HOST, postIdx, Optional.empty(), oPjGoodies);
                //Thread.sleep(1000); //requesting too fast?? #112
                postit.responseBody().get().close();
                break;
            }
            catch (Exception ignored) {
            }
        }


        //TODO:handle queryparams for minnesota twins #229
        //TODO: specify document ID to avoid duplicate docs
    }


}
