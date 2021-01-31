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
import java.io.IOException;
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


        ////////----------------------------------------------------------------------------------


        //Make the request
        ElasticSearch es = new ElasticSearch(Config.getParam("ELASTIC_SEARCH_HOST"));


        AbortableInputStream resbody = es.getDoc(Config.getParam("ELASTIC_SEARCH_HOST"),
                Config.getParam("ELASTIC_SEARCH_INDEX" + "/_search/"),
                Optional.of(dict));


        //parsing from hw1

        int offset = 0;
        int scratch = 0;
        byte[] bArr = new byte[resbody.available()]; //constructs array to dump read contents
        //https://www.baeldung.com/convert-input-stream-to-string
        while (scratch > -1) {
            try {
                scratch = resbody.read(bArr, offset, Math.min(1024, resbody.available())); //give me at the least 2KB
                offset += scratch;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String siteInfo = "";

        String text = new String(bArr, StandardCharsets.UTF_8);

        siteInfo = siteInfo + text;














        return Response.status(200).type("application/json").entity(results.toString(4))
                // below header is for CORS
                .header("Access-Control-Allow-Origin", "*").build();
    }
}

