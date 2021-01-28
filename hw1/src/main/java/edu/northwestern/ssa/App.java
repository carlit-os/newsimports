package edu.northwestern.ssa;


import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;




public class App {
    private static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID"); //why aren't these used
    private static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
    private static final String ELASTIC_SEARCH_HOST = System.getenv("ELASTIC_SEARCH_HOST");
    private static String COMMON_CRAWL_FILENAME = System.getenv("COMMON_CRAWL_FILENAME");
    private static final String ELASTIC_SEARCH_INDEX = System.getenv("ELASTIC_SEARCH_INDEX");



    //TODO add sources
    public static <S3ObjectSummary> void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Hello world!");

        int pageCount = 0;




        //Create S3CLient object
        S3Client sClient = S3Client.builder()
                .region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(30)).build())
                .build();



        //check for latest warc file if needed


        if (COMMON_CRAWL_FILENAME == null || COMMON_CRAWL_FILENAME == ""){


            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket("commoncrawl")
                    .prefix("crawl-data/CC-NEWS/${year}/${month}")
                    //%04d %02d
                    .build();

            ListObjectsV2Response result = sClient.listObjectsV2(request);

            //contents returns gives back list of objects

            //List<S3Object> holder = result.contents();

            //stream().sorted(Comparator.comparing(S3Object::key)).collect(Collectors.toList());

            /*
            for(S3Object obj : holder){
                obj.lastModified();
                obj.key();

            }*/

            //COMMON_CRAWL_FILENAME = holder.get(holder.size()-1).key();



            COMMON_CRAWL_FILENAME = "crawl-data/CC-NEWS/2017/02/CC-NEWS-20170202093341-00045.warc.gz";

            //https://stackoverflow.com/questions/21426843/get-last-element-of-stream-list-in-a-one-liner
        }


        //create request object
        GetObjectRequest sRequest = GetObjectRequest.builder()
                .bucket("commoncrawl")
                .key(COMMON_CRAWL_FILENAME)
                .build();





        //create file to write to
        //File warcHolder = new File("initialWARC.warc");

        //FileInputStream fis = new FileInputStream(warcHolder);

        //ResponseTransformer.toFile(warcHolder)

        //FileInputStream fis = sClient.getObject(sRequest, ResponseTransformer.toInputStream()); //consider streaming here instead of downloading

        //InputStream fis = ;




        //step 2 parsing
        ArchiveReader library = WARCReaderFactory.get(COMMON_CRAWL_FILENAME, sClient.getObject(sRequest), true); //pass inputstream of warcholder here



        //create index
        ElasticSearch es = new ElasticSearch("es"); //pair with es.close(); consider getenv in place


        es.createIndex(ELASTIC_SEARCH_INDEX, ELASTIC_SEARCH_HOST);

        //es.deleteIndex(ELASTIC_SEARCH_HOST,ELASTIC_SEARCH_INDEX); //remove when submitting
        //es.close(); // remove when submitting
        //sClient.close(); //remove when submitting

        //
        // each record is an HTTP response
        try {
            for (ArchiveRecord record : library) {
                Object wType = record.getHeader().getHeaderValue("WARC-Type"); //if this is response, we build strings + jsoup

                String siteInfo = "";

                if (wType.equals("response")) {
                    int offset = 0;
                    int scratch = 0;
                    byte[] bArr = new byte[record.available()]; //constructs array to dump read contents

                    while  (scratch > -1) {
                        try{
                            scratch = record.read(bArr, offset, Math.min(1024,record.available())); //give me at the least 2KB
                            offset += scratch;
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }


                    String text = new String(bArr, StandardCharsets.UTF_8);

                    siteInfo = siteInfo + text;

                    siteInfo.replace("\0", "");

                    String htmlRaw = siteInfo.substring(text.indexOf("\r\n\r\n") + 4);

                    Document htmlDoc = null;

                    try { //input is binary and unsupported
                        htmlDoc = Jsoup.parse(htmlRaw);
                        String url = record.getHeader().getUrl();
                        String title = htmlDoc.title();
                        String plainText = htmlDoc.text();

                        pageCount += 1;

                        //JSON construction https://www.tutorialspoint.com/json/json_java_example.htm

                        JSONObject goodies = new JSONObject();

                        goodies.put("txt", plainText);
                        goodies.put("title", title);
                        goodies.put("url", url);
                        //post


                        try {
                            es.postDoc(goodies, ELASTIC_SEARCH_INDEX, ELASTIC_SEARCH_HOST);
                        }catch (Exception e){
                            System.out.println("posting mistake"); //triggered
                        }


                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }





                }


            }

            System.out.println("This many responses:" + pageCount);
            //end of parse

            //es.deleteIndex();  //remove when submitting
            //es.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        //es.deleteIndex(ELASTIC_SEARCH_HOST,ELASTIC_SEARCH_INDEX); //remove when submitting
        es.close();
        sClient.close();

    }









}
//https://www.baeldung.com/convert-input-stream-to-string
//start from https://www.programcreek.com/java-api-examples/?api=org.archive.io.warc.WARCReaderFactory
//original function

                /*
                //while (record.available() != 0) {
                record.read(bArr); //read HTTP response record
                //}

                //bArr now has the contents of the record dumped in
                //convert bArr to string
                // byte[] to string https://mkyong.com/java/how-do-convert-byte-array-to-string-in-java/

                String transRecord = new String(bArr, StandardCharsets.UTF_8);

                 */