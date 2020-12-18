package com.cube.sequence;

import io.md.dao.Event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class SeqMgr {

    // assuming the incoming events are in right order sorted order
    public static Stream<Event> createSeqId(Stream<Event> goldenEvents , long size){

        SequenceGenerator gen = new SequenceGenerator(size);

        return populateSeqId(goldenEvents , gen , "");

    }

    // assuming the movedEventsBatch are in right sorted order
    public static Stream<Event> insertBetween(Optional<String> insertAfterSeqIdOpt , Optional<String> insertBeforeSeqIdOpt , Stream<Event> movedEventsBatch , long size) {

        int SeqIdlen = Math.max(insertAfterSeqIdOpt.orElse("").length() , insertBeforeSeqIdOpt.orElse("").length());

        String insertAfterSeqId = insertAfterSeqIdOpt.orElse((BaseCharUtils.PADDING_CHAR));
        //Make the length of the both seqId same by adding padding if required
        if(insertAfterSeqId.length()<SeqIdlen){
            insertAfterSeqId = insertAfterSeqId + BaseCharUtils.padding(SeqIdlen-insertAfterSeqId.length());
        }
        if(insertBeforeSeqIdOpt.isPresent() && insertBeforeSeqIdOpt.get().length()<SeqIdlen){
            String temp = insertBeforeSeqIdOpt.get();
            insertBeforeSeqIdOpt = Optional.of(temp + BaseCharUtils.padding(SeqIdlen-temp.length()));
        }

        long prevSeq =  BaseCharUtils.convertToNumber(insertAfterSeqId);
        long nextSeq =  insertBeforeSeqIdOpt.map(BaseCharUtils::convertToNumber).orElse((long) Math.pow(BaseCharUtils.BASE_LEN, SeqIdlen ));

        SequenceGenerator gen = null;
        String basePadding = null;
        if((nextSeq - prevSeq) > size){
            // can be fitted in between
            gen = new SequenceGenerator(prevSeq+1 , nextSeq , SeqIdlen , size);
            basePadding = "";
        }else{
            // it has to be padded to  insertAfter seqId
            gen = new SequenceGenerator(size);
            basePadding = insertAfterSeqId;
        }

        return populateSeqId(movedEventsBatch , gen , basePadding);
    }

    private static Stream<Event> populateSeqId(Stream<Event> movedEventsBatch , SequenceGenerator generator , String basePadding){
        Iterator<String> seqItr = generator.iterator();
        final String finalBasePadding = basePadding;
        final boolean padding = !finalBasePadding.isEmpty();
        Map<String, String> seqIdMap = new HashMap<>();

        return movedEventsBatch.map(e->{
            String seqId = seqIdMap.get(e.getReqId());
            if(seqId ==null){
                seqId = padding ? finalBasePadding + seqItr.next() : seqItr.next();
                seqIdMap.put(e.getReqId() , seqId);
            }
            e.setSeqId(seqId);
            return e;
        });
    }

}
