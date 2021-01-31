package edu.northwestern.ssa.api;

import edu.northwestern.ssa.AwsSignedRestRequest;
import edu.northwestern.ssa.Config;
import edu.northwestern.ssa.ElasticSearch;
import org.json.JSONArray;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("/search")
public class Search {
    //private static final String ELASTIC_SEARCH_HOST = System.getenv("ELASTIC_SEARCH_HOST");
    //private static final String ELASTIC_SEARCH_INDEX = System.getenv("ELASTIC_SEARCH_INDEX");

    /** when testing, this is reachable at http://localhost:8080/api/search?query=hello */
    @GET
    public Response getMsg(@QueryParam("query") String q, @QueryParam("title") String title, @QueryParam("url") String u, @QueryParam("txt") String txt, @QueryParam("language") String l, @QueryParam("date") String d) throws IOException {
        JSONArray results = new JSONArray();
        results.put(q);


        Map<String, String> dict = new HashMap<String, String>();
        dict.put("q","txt:northwestern");

        ////////----------------------------------------------------------------------------------


        //Make the request
        ElasticSearch es = new ElasticSearch(Config.getParam("ELASTIC_SEARCH_HOST"));


        AbortableInputStream resbody = es.getDoc(Config.getParam("ELASTIC_SEARCH_HOST"),
                Config.getParam("ELASTIC_SEARCH_INDEX") + "/_search/",
                Optional.of(dict));



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

        String dumvar = "";



        return Response.status(200).type("application/json").entity(results.toString(4))
                // below header is for CORS
                .header("Access-Control-Allow-Origin", "*").build();
    }
}
//sources

//parsing
// https://www.baeldung.com/java-buffered-reader
// https://stackoom.com/question/35Qvn/AmazonS-%E8%8E%B7%E5%8F%96%E8%AD%A6%E5%91%8A-S-AbortableInputStream-%E5%B9%B6%E9%9D%9E%E6%89%80%E6%9C%89%E5%AD%97%E8%8A%82%E9%83%BD%E4%BB%8ES-ObjectInputStream%E8%AF%BB%E5%8F%96-%E4%B8%AD%E6%AD%A2HTTP%E8%BF%9E%E6%8E%A5
// https://www.baeldung.com/convert-input-stream-to-string

// http://www.java2s.com/Code/Android/File/ToconverttheInputStreamtoStringweusetheBufferedReaderreadLinemethod.htm