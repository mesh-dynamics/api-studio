package io.cube.agent;

public class DisruptorData {

	private String disruptorOutputLocation;
	private String disruptorFileOutName;
	private long disruptorLogFileMaxSize;
	private int disruptorLogMaxBackup;
	private int ringBufferSize;
	private String recorderValue;

	public DisruptorData(String disruptorOutputLocation, String disruptorFileOutName, long disruptorLogFileMaxSize, int disruptorLogMaxBackup, int ringBufferSize, String recorderValue) {
		this.disruptorOutputLocation = disruptorOutputLocation;
		this.disruptorFileOutName = disruptorFileOutName;
		this.disruptorLogFileMaxSize = disruptorLogFileMaxSize;
		this.disruptorLogMaxBackup = disruptorLogMaxBackup;
		this.ringBufferSize = ringBufferSize;
		this.recorderValue = recorderValue;
	}


	public boolean compare(String disruptorOutputLocation, String disruptorFileOutName, long disruptorLogFileMaxSize, int disruptorLogMaxBackup, int ringBufferSize, String recorderValue) {
		return (!this.disruptorOutputLocation.equals(disruptorOutputLocation) || !this.disruptorFileOutName.equals(disruptorFileOutName) || this.disruptorLogFileMaxSize != disruptorLogFileMaxSize || this.disruptorLogMaxBackup != disruptorLogMaxBackup || this.ringBufferSize != ringBufferSize || !this.recorderValue.equalsIgnoreCase(recorderValue));
	}
}
