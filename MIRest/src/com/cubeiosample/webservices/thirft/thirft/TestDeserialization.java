package com.cubeiosample.webservices.thirft.thirft;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TJSONProtocol.Factory;

public class TestDeserialization {


   static class TBaseExclusionStrategy implements ExclusionStrategy {

    public boolean shouldSkipClass(Class<?> arg0) {
      return false;
    }

    public boolean shouldSkipField(FieldAttributes f) {
      return (f.getName().equals("__isset_bitfield"));
    }

  }


  public static void main(String[] args) {
    try {

      ListMovieResult listMovieResult = new ListMovieResult();
      MovieInfo movieInfo = new MovieInfo();
      movieInfo.setDisplayActors(Arrays.asList(new String[]{"NOLTE,JAYNE" , "MARX,ELVIS" , "WILLIS,HUMPHREY"}));
      movieInfo.setFilmId(23);
      movieInfo.setTitle("ANACONDA CONFESSIONS");
      movieInfo.setTimestamp(59388950755464L);

      BookInfo bookInfo = new BookInfo();
      Review review = new Review();
      review.setReviewer("Reviewer1");
      review.setText("An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!");
      bookInfo.addToReviews(review);

      Review review1 = new Review();
      review1.setReviewer("Reviewer2");
      review1.setText("Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.");
      bookInfo.addToReviews(review1);

      Rating rating = new Rating();
      rating.setReviewer("Reviewer2");
      rating.setStars(Short.parseShort("4"));

      bookInfo.addToRatings(rating);

      Rating rating1 = new Rating();
      rating1.setReviewer("Reviewer1");
      rating1.setStars(Short.parseShort("5"));
      bookInfo.addToRatings(rating1);

      Details details = new Details();
      details.setPages(200);
      details.setYear(Short.parseShort("1595"));
      details.setAuthor("William Shakespeare");
      details.setIsbn_13("123-1234567890");
      details.setPublisher("PublisherA");
      details.setIsbn_10("1234567890");
      details.setLanguage("English");
      details.setType("paperback");
      details.setId(23);

      bookInfo.setDetails(details);

      movieInfo.setBookInfo(bookInfo);
      listMovieResult.addToMovieInfoList(movieInfo);

      TSerializer tSerializer = new TSerializer(new Factory());
      System.out.println("Original :: " + tSerializer.toString(listMovieResult));

      Gson gson =new GsonBuilder()
          .setExclusionStrategies(new TBaseExclusionStrategy())
          //.serializeNulls() <-- uncomment to serialize NULL fields as well
          .create();
      String json = gson.toJson(listMovieResult);
      System.out.println(json);
      ListMovieResult fromJson = gson.fromJson(json , ListMovieResult.class);

      System.out.println("From Gson :: " + tSerializer.toString(fromJson));


    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
