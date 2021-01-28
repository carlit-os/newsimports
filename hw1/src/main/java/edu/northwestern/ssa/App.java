package edu.northwestern.ssa;


import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;

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

import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;



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

        //sClient.close(); //remove when submitting


        //check for latest warc file if needed


        if (COMMON_CRAWL_FILENAME == null || COMMON_CRAWL_FILENAME.equals("")){

            LocalDate today = LocalDate.now();

            Month monthObj = today.getMonth();

            int monthNum = monthObj.getValue();

            String strToday = today.toString();

            String curMonth = strToday.substring(5,7);

            String curDay = strToday.substring(8,10);

            String curYear = strToday.substring(0,4);

            /*
            if (curDay.equals("01")){
                if (curMonth.equals("01")){
                    curMonth = "12";
                }


            }*/

            //https://stackoverflow.com/questions/8027265/how-to-list-all-aws-s3-objects-in-a-bucket-using-java
            //https://docs.aws.amazon.com/AmazonS3/latest/dev/ListingObjectKeysUsingJava.html
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket("commoncrawl")
                    .prefix("crawl-data/CC-NEWS/"+curYear+"/"+curMonth)
                    .build();


            ListObjectsV2Response response = sClient.listObjectsV2(request);


            List<S3Object> holder = response.contents();

            
            // create a string made up of n copies of string s
            String largeKey = String.join("", Collections.nCopies(49, "a"));


            //int trade = 0 //delete when sub
            //TODO add sources
            for (S3Object obj : holder){
                String trimKey = obj.key().substring(0,49);
                String trimLarge = largeKey.substring(0,49);
                //TODO remove printouts
                //TODO clear indicies
                if(trimKey.compareTo(trimLarge) > 0){
                    //System.out.println("took "+obj.key()+" handed off " + largeKey);
                    largeKey = obj.key();
                }else {
                    //System.out.println("kept "+ largeKey+" disregarded " + obj.key());
                }


            }

            //contents returns gives back list of objects

            //List<S3Object> holder = result.contents();


            /*
            for(ListObjectsV2Response page : response) {
                page.contents().forEach((S3Object obj) -> {
                    System.out.println(obj.key());
                });

                //obj.lastModified();
                //obj.key();

            }*/


            COMMON_CRAWL_FILENAME = largeKey;

            //https://stackoverflow.com/questions/21426843/get-last-element-of-stream-list-in-a-one-liner
        }


        //create request object
        GetObjectRequest sRequest = GetObjectRequest.builder()
                .bucket("commoncrawl")
                .key(COMMON_CRAWL_FILENAME)
                .build();


        //start from https://www.programcreek.com/java-api-examples/?api=org.archive.io.warc.WARCReaderFactory


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
                    //https://www.baeldung.com/convert-input-stream-to-string
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

//COMMON_CRAWL_FILENAME=crawl-data/CC-NEWS/2017/02/CC-NEWS-20170202093341-00045.warc.gz tiny warc file





