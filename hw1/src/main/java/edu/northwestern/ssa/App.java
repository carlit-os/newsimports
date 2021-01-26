package edu.northwestern.ssa;


import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


public class App {
    private static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
    private static final String ELASTIC_SEARCH_HOST = System.getenv("ELASTIC_SEARCH_HOST");
    private static String COMMON_CRAWL_FILENAME = System.getenv("COMMON_CRAWL_FILENAME");
    private static final String ELASTIC_SEARCH_INDEX = System.getenv("ELASTIC_SEARCH_INDEX");




    //TODO add sources
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Hello world!");

        int pageCount = 0;




        //Create S3CLient object
        S3Client sClient = S3Client.builder()
                .region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(30)).build())
                .build();


        //check for latest warc file if needed
        //if (COMMON_CRAWL_FILENAME == null){
        //    ;
        //}

        //create request object
        GetObjectRequest sRequest = GetObjectRequest.builder()
                .bucket("commoncrawl")
                .key(COMMON_CRAWL_FILENAME)
                .build();

        //create file to write to
        File warcHolder = new File("initialWARC.warc");


        sClient.getObject(sRequest, ResponseTransformer.toFile(warcHolder));



        //-------------------------------------------------------------------------------------------------------------
        //-------------------------------------------------------------------------------------------------------------



        //step 2 parsing
        ArchiveReader library = WARCReaderFactory.get(warcHolder);

        //create index
        ElasticSearch es = new ElasticSearch("es"); //pair with es.close(); consider getenv in place


        es.createIndex(ELASTIC_SEARCH_INDEX);



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
                            scratch = record.read(bArr, offset, bArr.length);
                            offset += scratch;
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                // String rawStr = bArr.toString();


                //String rawStr = new String(bArr, StandardCharsets.UTF_8); //consider experimenting with charsets on string declr and inputstream declr
                //InputStream tempStream = new ByteArrayInputStream(record);

                //InputStream inputStream = new ByteArrayInputStream(rawStr.getBytes());

                //ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                //record.dump(buffer);

                /* remove if dumping works
                int nRead;
                byte[] data = new byte[1024]; //try using bArr in place

                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                */

                //buffer.flush();
                //byte[] byteArray = buffer.toByteArray();

                    String text = new String(bArr, StandardCharsets.UTF_8);

                    siteInfo = siteInfo + text;

                //assertThat(text, equalTo(originalString)); looks like a unit test
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

                //goodies


                ////////////////step 3 jsoup + call to post






                    String htmlRaw = siteInfo.substring(text.indexOf("\r\n\r\n") + 4);
                    Document htmlDoc = Jsoup.parse(htmlRaw);
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
                    es.postDoc(goodies);


                }


            }

            System.out.println("This many responses:" + pageCount);
            //end of parse
            sClient.close();
            //es.deleteIndex();  //remove when submitting
            es.close();

            warcHolder.delete();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            sClient.close();
            es.deleteIndex();
            es.close();

            warcHolder.delete();
        }

    }









}
