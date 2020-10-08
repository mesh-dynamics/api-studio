package io.md;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;

import delight.fileupload.FileUpload;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Builder;
import okhttp3.RequestBody;
import okio.Buffer;

public class TestFileUpload {

	public static void main(String[] args) {
		try {

			byte[] decoded = Base64.getDecoder().decode(Files.readAllBytes(Paths.
				get("/Users/ravivj/md-commons/src/test/resources/encodedMultipart.txt")));

			System.out.println(new String(decoded));


			/*List<FileItem> iterator = FileUpload.parse(decoded
			 , "multipart/form-data; boundary=--------------------------855860037718019462118012");
*/
			List<FileItem> iterator = FileUpload
				.parse(decoded , "multipart/form-data; boundary=--------------------------604081540345378243351723");


			Builder builder =  new MultipartBody.Builder();

			iterator.forEach(item -> {
				if (item.isFormField()) {
					builder.addFormDataPart(item.getFieldName(), item.getString());
				} else {
					try {
						builder.addFormDataPart(item.getFieldName(),
							item.getName(),  RequestBody.create(IOUtils.toByteArray(item.getInputStream()),
								MediaType.parse(item.getContentType())));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

			});

			final Buffer buffer = new Buffer();
			builder.build().writeTo(buffer);
			System.out.println(new String(Base64.getEncoder().encode(buffer.readByteArray())));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
