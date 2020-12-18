package com.cube.sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class BaseCharUtils {
    static final String alphabetsSmallLetters = IntStream.rangeClosed((int)'a' , (int)'z' ).mapToObj(c->Character.valueOf((char) c).toString()).collect(Collectors.joining());
    static final String alphabetsCapitalLetters = alphabetsSmallLetters.toUpperCase();
    static final String numericLetters = IntStream.rangeClosed(0,9).mapToObj(Integer::toString).collect(Collectors.joining()); //"0123456789";

    // 0-9A-Za-z
    private static String VALID_SEQ_CHARS = numericLetters + alphabetsCapitalLetters + alphabetsSmallLetters ;
    public static final String BASE_CHARS = VALID_SEQ_CHARS.chars().distinct().sorted().mapToObj(c->Character.valueOf((char)c).toString()).collect(Collectors.joining());
    public static final int BASE_LEN = BASE_CHARS.length();
    public static final String PADDING_CHAR = String.valueOf(BASE_CHARS.charAt(0));
    private static final Map<Character , Integer> BASE_CHARS_POSITIONS;// = BASE_CHARS.chars().mapToObj(c->(char)c).collect(Collectors.toMap(c->c , c->BASE_CHARS.indexOf(c)));

    private static final Map<Integer , String> paddingCache = new HashMap<>();

    static {
        BASE_CHARS_POSITIONS = new HashMap<>();
        char[] arr = BASE_CHARS.toCharArray();
        for(int i=0 ; i<arr.length ; i++){
            BASE_CHARS_POSITIONS.put(arr[i] , i);
        }
    }


    static String convertToSeqId(long value , int stringLength){

        StringBuilder buff = new StringBuilder();
        for(int i=0 ; i<stringLength ; i++){
            long reminder = value % BASE_LEN ;
            value = value / BASE_LEN;
            buff.append(BASE_CHARS.charAt((int)reminder));
        }
        // pad the remaining number with BASE[0] to make the total string length == stringLength
        buff.append(padding(stringLength - buff.length()) );
        return buff.reverse().toString();
    }

    static long convertToNumber(String seqId){
        long val=0;
        for(char c : seqId.toCharArray()){
            val = val *  BASE_LEN + BASE_CHARS_POSITIONS.get(c);
        }
        return val;
    }

    public static String padding(int length){

        if(length==0) return "";

        String padding = paddingCache.get(length);
        if(padding==null){
            padding = PADDING_CHAR.repeat(length);
            paddingCache.put(length , padding);
        }
        return padding;
    }



}
