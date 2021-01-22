package edu.northwestern.ssa;


import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.*;
import java.nio.charset.Charset;
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

        //step 2 parsing
        ArchiveReader library = WARCReaderFactory.get(warcHolder);

        //each record is an HTTP response

        //-------------------------------------------------------------------------------------------------------------
        //-------------------------------------------------------------------------------------------------------------


        for(ArchiveRecord record: library){

            //byte[] bArr = new byte[record.available()]; //constructs array to dump read contents

            //while (record.available() != 0) {
            //record.read(bArr); //read HTTP response record
            //}

            String responseStr = record.toString(); //convert ot string type



            Object wType = record.getHeader().getHeaderValue("WARC-Type");
            //if this is a response, we care about it

            String url = record.getHeader().getUrl(); //examine URL
            System.out.println(url);

            //inputstream take2 method4

            InputStream inputStream = new ByteArrayInputStream(responseStr.getBytes());

            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader
                    (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }

            System.out.println(textBuilder.toString());


        }


        //end of parse

        warcHolder.delete();

    }


}
