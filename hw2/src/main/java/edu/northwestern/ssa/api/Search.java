package edu.northwestern.ssa.api;

import edu.northwestern.ssa.AwsSignedRestRequest;
import edu.northwestern.ssa.Config;
import edu.northwestern.ssa.ElasticSearch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import org.jsoup.nodes.Document;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Path("/search")
public class Search {
    //private static final String ELASTIC_SEARCH_HOST = System.getenv("ELASTIC_SEARCH_HOST");
    //private static final String ELASTIC_SEARCH_INDEX = System.getenv("ELASTIC_SEARCH_INDEX");
        //@from is offset
        //@size is count
    /** when testing, this is reachable at http://localhost:8080/api/search?query=hello */

    //TODO lang or language?
    //TODO 400 errors are our fault 500 are Tarzia's fault
    @GET
    public Response getMsg(@QueryParam("query") String q, @QueryParam("language") String l, @QueryParam("date") String d, @QueryParam("count") String c, @QueryParam("offset") String o) throws IOException {
        int status = 200;


        JSONArray results = new JSONArray();
        //results.put(q);


        Map<String, String> dict = new HashMap<String, String>();

        //TODO lang is key in dictionary

        ////////-------------------------------------------------------------------------------------------------------
        ////////--------------------------------------------------------------------------------------------------------
        //build lucene query
        List<String> qargs = new ArrayList<String>();


        if(q != null){
            q = q.replaceAll(" "," AND ");
        }else{
            status = 400;
        }


        qargs.add("txt:("+q+")");


        if(l != null){
            qargs.add("lang:"+l);
        }
        if(d != null){
            qargs.add("date:"+d);
        }

        String lucene = String.join(" AND ", qargs);

        ////////--------------------------------------------------------------------------------------------------------
        ////////--------------------------------------------------------------------------------------------------------


        //put query params into dict
        //parameters are keys
        dict.put("q",lucene);
        //key should be lang
        if(c != null){
            dict.put("size",c);
        }
        if(o != null){
            dict.put("from",o);
        }

        dict.put("track_total_hits", "true");  //total results



        ////////----------------------------------------------------------------------------------


        //Make the request
        ElasticSearch es = new ElasticSearch(Config.getParam("ELASTIC_SEARCH_HOST"));


        JSONObject jObj = es.getDoc(Config.getParam("ELASTIC_SEARCH_HOST"),
                Config.getParam("ELASTIC_SEARCH_INDEX") + "/_search/",
                Optional.of(dict));

        JSONObject grabber = (JSONObject) jObj.get("hits");

        JSONArray deepGrabber = (JSONArray) grabber.get("hits"); //basically array of articles

        JSONArray artList = new JSONArray();

        int returned_results=deepGrabber.length();

        for (int i = 0; i < deepGrabber.length(); i++) {
            JSONObject meta = deepGrabber.getJSONObject(i);
            JSONObject article = meta.getJSONObject("_source");


            String title = (String) article.get("title");
            String url = (String) article.get("url");

            boolean dateFail = true;
            String date = null;

            boolean langFail = true;
            String gLang = null;

            try{
                date = (String) article.get("date");
                dateFail = false;
            }catch (Exception je){
            }


            try{
                gLang = (String) article.get("lang");
                langFail = false;
            }catch (Exception je){
            }




            String txt = (String) article.get("txt");

            JSONObject art = new JSONObject();
            art.put("title",title);
            art.put("url",url);
            art.put("txt",txt);
            art.put("date",date);
            art.put("lang",gLang);

            artList.put(art);

        }

        //should have articles: list filled up

        JSONObject gift = new JSONObject();

        gift.put("returned_results",returned_results);


        JSONObject metaTotal = (JSONObject) grabber.get("total");

        int total = (int) metaTotal.get("value");

        gift.put("total_results",total);
        gift.put("articles",artList);

        //String dummu = ""; break here to see vars

        if (status==400) {
            return Response.status(status).type("application/json").entity("'query' is missing from url.")
                    // below header is for CORS
                    .header("Access-Control-Allow-Origin", "*").build();
        }else {
            return Response.status(status).type("application/json").entity(gift.toString(4))
                    // below header is for CORS
                    .header("Access-Control-Allow-Origin", "*").build();
        }

    }
}
//sources

//parsing
// https://www.baeldung.com/java-buffered-reader
// https://stackoom.com/question/35Qvn/AmazonS-%E8%8E%B7%E5%8F%96%E8%AD%A6%E5%91%8A-S-AbortableInputStream-%E5%B9%B6%E9%9D%9E%E6%89%80%E6%9C%89%E5%AD%97%E8%8A%82%E9%83%BD%E4%BB%8ES-ObjectInputStream%E8%AF%BB%E5%8F%96-%E4%B8%AD%E6%AD%A2HTTP%E8%BF%9E%E6%8E%A5
// https://www.baeldung.com/convert-input-stream-to-string

// http://www.java2s.com/Code/Android/File/ToconverttheInputStreamtoStringweusetheBufferedReaderreadLinemethod.htm

///http://localhost:8080/api/search?query=hello+world&language=en&date=2021-01-25&count=1