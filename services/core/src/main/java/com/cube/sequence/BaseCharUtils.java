/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.sequence;

import java.math.BigInteger;
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
    public static final BigInteger BASE_LEN_BI = BigInteger.valueOf(BASE_CHARS.length());
    public static final String PADDING_CHAR = String.valueOf(BASE_CHARS.charAt(0));
    private static final Map<Character , BigInteger> BASE_CHARS_POSITIONS;// = BASE_CHARS.chars().mapToObj(c->(char)c).collect(Collectors.toMap(c->c , c->BASE_CHARS.indexOf(c)));

    private static final Map<Integer , String> paddingCache = new HashMap<>();

    static {
        BASE_CHARS_POSITIONS = new HashMap<>();
        char[] arr = BASE_CHARS.toCharArray();
        for(int i=0 ; i<arr.length ; i++){
            BASE_CHARS_POSITIONS.put(arr[i] , BigInteger.valueOf(i));
        }
    }


    static String convertToSeqId(BigInteger value , int stringLength){

        StringBuilder buff = new StringBuilder();
        for(int i=0 ; i<stringLength ; i++){
            BigInteger[] divideRem = value.divideAndRemainder(BASE_LEN_BI);
            int remainder = divideRem[1].intValue() ;
            value = divideRem[0];
            buff.append(BASE_CHARS.charAt(remainder));
        }
        // pad the remaining number with BASE[0] to make the total string length == stringLength
        buff.append(padding(stringLength - buff.length()) );
        return buff.reverse().toString();
    }

    static BigInteger convertToNumber(String seqId){
        BigInteger val= BigInteger.ZERO;
        for(char c : seqId.toCharArray()){
            val = val.multiply(BASE_LEN_BI).add(BASE_CHARS_POSITIONS.get(c));
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
