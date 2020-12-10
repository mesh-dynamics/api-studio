/**
 * @param {*} object Non empty object which contents key value pairs
 * @param {*} key Key name whose value needs to extracted
 */

const getParameterCaseInsensitive = (object, key) => {
    return object[
        Object.keys(object)
        .find(k => k.toLowerCase() === key.toLowerCase())
    ];
}


const Base64Binary = {
	_keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",
	
	/* will return a  Uint8Array type */
	decodeArrayBuffer: function(input) {
		var bytes = (input.length/4) * 3;
		var ab = new ArrayBuffer(bytes);
		this.decode(input, ab);
		
		return ab;
	},

	removePaddingChars: function(input){
		var lkey = this._keyStr.indexOf(input.charAt(input.length - 1));
		if(lkey == 64){
			return input.substring(0,input.length - 1);
		}
		return input;
	},

	decode: function (input, arrayBuffer) {
		//get last chars to see if are valid
		input = this.removePaddingChars(input);
		input = this.removePaddingChars(input);

		var bytes = parseInt((input.length / 4) * 3, 10);
		
		var uarray;
		var chr1, chr2, chr3;
		var enc1, enc2, enc3, enc4;
		var i = 0;
		var j = 0;
		
		if (arrayBuffer)
			uarray = new Uint8Array(arrayBuffer);
		else
			uarray = new Uint8Array(bytes);
		
		input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");
		
		for (i=0; i<bytes; i+=3) {	
			//get the 3 octects in 4 ascii chars
			enc1 = this._keyStr.indexOf(input.charAt(j++));
			enc2 = this._keyStr.indexOf(input.charAt(j++));
			enc3 = this._keyStr.indexOf(input.charAt(j++));
			enc4 = this._keyStr.indexOf(input.charAt(j++));
	
			chr1 = (enc1 << 2) | (enc2 >> 4);
			chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
			chr3 = ((enc3 & 3) << 6) | enc4;
	
			uarray[i] = chr1;			
			if (enc3 != 64) uarray[i+1] = chr2;
			if (enc4 != 64) uarray[i+2] = chr3;
		}
	
		return uarray;	
    },
    encode: function(dataArr){
        var encoder = new TextEncoder("ascii");
        var decoder = new TextDecoder("ascii");
        var base64Table = encoder.encode(this._keyStr);

        var padding = dataArr.byteLength % 3;
        var len = dataArr.byteLength - padding;
        padding = padding > 0 ? (3 - padding) : 0;
        var outputLen = ((len/3) * 4) + (padding > 0 ? 4 : 0);
        var output = new Uint8Array(outputLen);
        var outputCtr = 0;
        for(var i=0; i<len; i+=3){              
            var buffer = ((dataArr[i] & 0xFF) << 16) | ((dataArr[i+1] & 0xFF) << 8) | (dataArr[i+2] & 0xFF);
            output[outputCtr++] = base64Table[buffer >> 18];
            output[outputCtr++] = base64Table[(buffer >> 12) & 0x3F];
            output[outputCtr++] = base64Table[(buffer >> 6) & 0x3F];
            output[outputCtr++] = base64Table[buffer & 0x3F];
        }
        if (padding == 1) {
            var buffer = ((dataArr[len] & 0xFF) << 8) | (dataArr[len+1] & 0xFF);
            output[outputCtr++] = base64Table[buffer >> 10];
            output[outputCtr++] = base64Table[(buffer >> 4) & 0x3F];
            output[outputCtr++] = base64Table[(buffer << 2) & 0x3F];
            output[outputCtr++] = base64Table[64];
        } else if (padding == 2) {
            var buffer = dataArr[len] & 0xFF;
            output[outputCtr++] = base64Table[buffer >> 2];
            output[outputCtr++] = base64Table[(buffer << 4) & 0x3F];
            output[outputCtr++] = base64Table[64];
            output[outputCtr++] = base64Table[64];
        }
        
        var ret = decoder.decode(output);
        output = null;
        dataArr = null;
        return ret;
    }
}

const isJsonOrGrpcMime = (contentType) => {
    return contentType && (contentType.toLowerCase().indexOf("json") || contentType.toLowerCase().indexOf("grpc"));
}

module.exports = { getParameterCaseInsensitive, isJsonOrGrpcMime, Base64Binary };