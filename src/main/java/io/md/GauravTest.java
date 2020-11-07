package io.md;

import org.apache.http.entity.ContentType;

public class GauravTest {

    public static  void main(String[] myArgs){

        ContentType ct = ContentType.parse("applicationwww/vnd.accpac.simply.aso;charset1=utf-81");


        System.out.println("Media Type is "+ ct.getMimeType());


    }
}
