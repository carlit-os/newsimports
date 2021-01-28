package edu.northwestern.ssa;


import org.json.JSONObject;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.io.IOException;
import java.util.Optional;


public class ElasticSearch extends AwsSignedRestRequest {

    //private final String serviceName;


    /**
     * @param serviceName would be "es" for Elasticsearch
     */
    ElasticSearch(String serviceName) {
        super(serviceName);

        //this.serviceName=serviceName; is this needed ??

    }


    //create index method
    public void createIndex(String index, String host) throws IOException {
        HttpExecuteResponse table = this.restRequest(SdkHttpMethod.PUT,host,index, java.util.Optional.empty());

        table.responseBody().get().close(); //do I need to close this when I create an index or just when posting??
    }

    //delete index method
    public void deleteIndex(String host,String index) throws IOException {
        HttpExecuteResponse erase = this.restRequest(SdkHttpMethod.DELETE,host,index, java.util.Optional.empty());

        erase.responseBody().get().close(); // is this necessary?
    }



    //postdoc method
    public void postDoc(JSONObject jGoodies, String index, String host) throws IOException, InterruptedException { //consider returning the while loop for evanston
        String postIdx = index + "/_doc/";

        Optional<JSONObject> oPjGoodies= Optional.of(jGoodies);

        HttpExecuteResponse postit = this.restRequest(SdkHttpMethod.POST, host, postIdx, java.util.Optional.empty(), oPjGoodies);

       // System.out.println("Posting status code" + postit.httpResponse().statusCode());
        postit.responseBody().get().close();
        //Thread.sleep(50); //requesting too fast?? #112


        //this.restRequest(SdkHttpMethod.POST, host , postIdx, java.util.Optional.empty(), oPjGoodies).responseBody().get().close(); condensed!!
        //postit.responseBody().get().close();
        //System.out.println("Posting status code" + postit.httpResponse().statusCode());

        //while(true) {
           // try {



                //break;
            //}
            //catch (Exception ignored) {
            //}
        //}


        //TODO:handle queryparams for minnesota twins #229
        //TODO: specify document ID to avoid duplicate docs
    }


}
