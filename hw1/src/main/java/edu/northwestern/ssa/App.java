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

import java.io.File;
import java.io.IOException;
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
        File warcHolder = new File("initialWARC.txt");


        sClient.getObject(sRequest, ResponseTransformer.toFile(warcHolder));

        //s requests have concluded
        sClient.close();

        //step 2 parsing
        ArchiveReader library = WARCReaderFactory.get(warcHolder);

        //each record is an HTTP response
        for(ArchiveRecord record: library){
            byte[] bArr = new byte[record.available()]; //constructs array to dump read contents


            record.read(bArr); //read HTTP response record


            //bArr now has the contents of the record dumped in
            //convert bArr to string

        }


        //end of parse

        warcHolder.delete();

    }


}
