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
