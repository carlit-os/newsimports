package edu.northwestern.ssa;


import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
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

    public static void main(String[] args) throws IOException {
        System.out.println("Hello world!");

        //Create S3CLient object
        S3Client sClient = S3Client.builder()
                .region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMinutes(30)).build())
                .build();

        //create request object
        GetObjectRequest sRequest = GetObjectRequest.builder()
                .bucket("commoncrawl")
                .key("crawl-data/CC-NEWS/2017/02/CC-NEWS-20170202093341-00045.warc.gz")
                .build();

        //create file to write to
        File warcHolder = new File("initialWARC.warc");


        sClient.getObject(sRequest, ResponseTransformer.toFile(warcHolder));

        //s requests have concluded
        sClient.close();
        //-------------------------------------------------------------------------------------------------------------
        //-------------------------------------------------------------------------------------------------------------
        //step 2 parsing
        ArchiveReader library = WARCReaderFactory.get(warcHolder);

        //each record is an HTTP response
        for(ArchiveRecord record: library){
            Object wType = record.getHeader().getHeaderValue("WARC-Type"); //if this is response, we jsoup
            byte[] bArr = new byte[record.available()]; //constructs array to dump read contents


            // String rawStr = bArr.toString();


            //String rawStr = new String(bArr, StandardCharsets.UTF_8); //consider experimenting with charsets on string declr and inputstream declr
            //InputStream tempStream = new ByteArrayInputStream(record);

            //InputStream inputStream = new ByteArrayInputStream(rawStr.getBytes());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            record.dump(buffer);

            /* remove if dumping works
            int nRead;
            byte[] data = new byte[1024]; //try using bArr in place

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            */

            buffer.flush();
            byte[] byteArray = buffer.toByteArray();

            String text = new String(byteArray, StandardCharsets.UTF_8);

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






           ////////////////step 3 jsoup
            if (wType.equals("response")) {
                String htmlRaw = text.substring(text.indexOf("\r\n\r\n") + 4);
                Document htmlDoc = Jsoup.parse(htmlRaw);
                String url = record.getHeader().getUrl();
                String title = htmlDoc.title();
                String plainText = htmlDoc.text();
            }



        }


        //end of parse

        warcHolder.delete();

    }









}
