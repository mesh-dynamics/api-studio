package com.cube.sequence;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.md.dao.Event;

public class SeqMgr {

	// assuming the incoming events are in right order sorted order
	public static Stream<Event> createSeqId(Stream<Event> goldenEvents , long size){

		SequenceGenerator gen = new SequenceGenerator(size);

		return populateSeqId(goldenEvents , gen);

	}

	// assuming the movedEventsBatch are in right sorted order
	public static Stream<Event> insertBetween(Optional<String> insertAfterSeqIdOpt , Optional<String> insertBeforeSeqIdOpt , Stream<Event> movedEventsBatch , long size) {

		SequenceGenerator gen = getGenerator(insertAfterSeqIdOpt , insertBeforeSeqIdOpt ,size);

		return populateSeqId(movedEventsBatch , gen);
	}

	static SequenceGenerator getGenerator(Optional<String> insertAfterSeqIdOpt , Optional<String> insertBeforeSeqIdOpt ,  long size){
		int SeqIdlen = Math.max(insertAfterSeqIdOpt.orElse("").length() , insertBeforeSeqIdOpt.orElse("").length());

		String insertAfterSeqId = insertAfterSeqIdOpt.orElse((BaseCharUtils.PADDING_CHAR));
		//Make the length of the both seqId same by adding padding if required
		if(insertAfterSeqId.length()<SeqIdlen){
			insertAfterSeqId = insertAfterSeqId + BaseCharUtils.padding(SeqIdlen-insertAfterSeqId.length());
		}
		insertBeforeSeqIdOpt = insertBeforeSeqIdOpt.map(v->v+ BaseCharUtils
			.padding(Math.max(0 , SeqIdlen - v.length())));

		BigInteger prevSeq =  BaseCharUtils.convertToNumber(insertAfterSeqId);
		BigInteger nextSeq =  insertBeforeSeqIdOpt.map(BaseCharUtils::convertToNumber).orElse(
			BaseCharUtils.BASE_LEN_BI.pow(SeqIdlen));

		SequenceGenerator gen = null;
		if(nextSeq.subtract(prevSeq).compareTo(BigInteger.valueOf(size)) > 0){
			// can be fitted in between
			// prevSeq and nextSeq both are non inclusive
			gen = new SequenceGenerator(prevSeq , nextSeq , SeqIdlen , size);
		}else{
			int nd = (int) Math.ceil(Math.log ((size+1)/(nextSeq.subtract(prevSeq).longValueExact()))/Math.log (
				BaseCharUtils.BASE_LEN));
			gen = new SequenceGenerator(prevSeq.multiply(BaseCharUtils.BASE_LEN_BI.pow(nd))  , nextSeq.multiply(
				BaseCharUtils.BASE_LEN_BI.pow(nd)) , SeqIdlen+nd , size );
		}
		return gen;
	}

	private static Stream<Event> populateSeqId(Stream<Event> movedEventsBatch , SequenceGenerator generator){
		Iterator<String> seqItr = generator.iterator();
		Map<String, String> seqIdMap = new HashMap<>();

		return movedEventsBatch.map(e->{
			String seqId = seqIdMap.get(e.getReqId());
			if(seqId ==null){
				seqId = seqItr.next();
				seqIdMap.put(e.getReqId() , seqId);
			}
			e.setSeqId(seqId);
			return e;
		});
	}

}