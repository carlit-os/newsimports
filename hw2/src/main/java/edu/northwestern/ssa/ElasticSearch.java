package edu.northwestern.ssa;

import org.json.JSONObject;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

public class ElasticSearch extends AwsSignedRestRequest{


    /**
     * @param serviceName would be "es" for Elasticsearch
     */
    public ElasticSearch(String serviceName) {
        super(serviceName);
    }





    public JSONObject getDoc(String host , String idx, Optional<Map<String,String>> dict) throws IOException {

        HttpExecuteResponse getit = this.restRequest(SdkHttpMethod.GET,host, idx, dict);

        AbortableInputStream resbody = getit.responseBody().get();

        BufferedReader reader = new BufferedReader(new InputStreamReader(resbody));

        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                resbody.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String siteInfo = sb.toString();

        JSONObject jObj = new JSONObject(siteInfo);

        return jObj;
    }




}
