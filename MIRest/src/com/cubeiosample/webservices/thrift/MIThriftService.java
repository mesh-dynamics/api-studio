package com.cubeiosample.webservices.thrift;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cubeiosample.webservices.rest.jersey.Config;
import com.cubeiosample.webservices.rest.jersey.ListMoviesCache;
import com.cubeiosample.webservices.rest.jersey.MovieRentals;

import io.cube.agent.CommonUtils;
import io.cube.tracing.thriftjava.Span;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;

public class MIThriftService implements  MIThrift.Iface {

    private static Logger LOGGER  = Logger.getLogger(MIThriftService.class);
    private static JaegerTracer tracer;
    private static Config config;

    private static MovieRentals mv;
    private static ListMoviesCache lmc;

    static {
        LOGGER = Logger.getLogger(MIThriftService.class);
        BasicConfigurator.configure();
    }

    @Override
    public boolean healthCheck() throws TException {
        return true;
    }

    @Override
    public ListMovieResult listMovies(String filmName, String keyWord, String actor, Span span)
            throws GenericMIRestException, TException {
        JSONArray films;
        ListMovieResult listMovieResult = new ListMovieResult();
        try (Scope scope = CommonUtils.startServerSpan(span, "listMovies")) {
            LOGGER.info("Current span In List Movies (Started From Null):: " + scope.span().toString());
            films = lmc.getMovieList(filmName);
            if (films != null) {
                films.forEach(jsonObj -> listMovieResult.addToMovieInfoList(convertJsonToObject((JSONObject)jsonObj))) ;
            }
            films = lmc.getMovieList(keyWord);
            if (films != null) {
                films.forEach(jsonObj -> listMovieResult.addToMovieInfoList(convertJsonToObject((JSONObject)jsonObj))) ;
            }
        } catch (Exception e) {
            LOGGER.error("Error Occurred while fetching list of movies :: " + e.getMessage());
        }
        return listMovieResult;
    }

    private List<String> extractStringList(JSONArray jsonArray) {
        List<String> toReturn = new ArrayList<>();
        jsonArray.forEach(element -> toReturn.add((String) element));
        return toReturn;
    }

    private List<Integer> extractIntegerList(JSONArray jsonArray) {
        List<Integer> toReturn = new ArrayList<>();
        jsonArray.forEach(element -> toReturn.add((Integer) element));
        return toReturn;
    }


    public MovieInfo convertJsonToObject(JSONObject obj) {
        MovieInfo mInfo = new MovieInfo();
        mInfo.filmId = obj.getInt("film_id");
        if (obj.has("display_actors"))
            mInfo.displayActors = extractStringList(obj.getJSONArray("display_actors"));
        if (obj.has("film_counts")) {
            try {
                mInfo.filmCounts = extractIntegerList(obj.getJSONArray("film_counts"));
            } catch (Exception e) {
                LOGGER.error("Error while extracting film counts :: " + e.getMessage());
            }
        }
            mInfo.title = obj.getString("title");
        mInfo.timestamp = obj.getLong("timestamp");
        if (obj.has("actors_firstnames"))
            mInfo.actorsFirstNames = extractStringList(obj.getJSONArray("actors_firstnames"));
        if (obj.has("actors_lastnames"))
            mInfo.actorsLastNames = extractStringList(obj.getJSONArray("actors_lastnames"));
        if (obj.has("book_info")) {
            BookInfo bookInfo = new BookInfo();
            JSONObject bookInfoObj = obj.getJSONObject("book_info");
            //bookInfo.id = bookInfoObj.getInt("id");
            if (bookInfoObj.has("reviews")) {
                JSONArray reviewsArray = bookInfoObj.getJSONArray("reviews");
                reviewsArray.forEach(reviewsObj -> {
                    Review review = new Review();
                    review.reviewer = ((JSONObject) reviewsObj).getString("reviewer");
                    review.text = ((JSONObject) reviewsObj).getString("text");
                    bookInfo.addToReviews(review);
                });
            }
            if (bookInfoObj.has("ratings")) {
                JSONObject ratinsObj = bookInfoObj.getJSONObject("ratings");
                ratinsObj.keySet().forEach(reviewer -> {
                    Rating rating = new Rating();
                    rating.reviewer = reviewer;
                    rating.stars = (short) ratinsObj.getInt(reviewer);
                    bookInfo.addToRatings(rating);
                });
            }
            if (bookInfoObj.has("details")) {
                JSONObject detailsObj = bookInfoObj.getJSONObject("details");
                Details details = new Details();
                details.author = detailsObj.getString("author");
                details.year = (short) detailsObj.getInt("year");
                details.pages = detailsObj.getInt("pages");
                details.isbn_13 = detailsObj.getString("ISBN-13");
                details.publisher = detailsObj.getString("publisher");
                details.isbn_10 = detailsObj.getString("ISBN-10");
                details.language = detailsObj.getString("language");
                details.type = detailsObj.getString("type");
                details.id = detailsObj.getInt("id");
                bookInfo.details = details;
            }
            mInfo.bookInfo = bookInfo;
        }

        return mInfo;
    }



    @Override
    public RentMovieResult rentMovie(RentalInfo rentalInfo, Span span) throws GenericMIRestException, TException {
        int customerId = rentalInfo.customerId;
        int duration = rentalInfo.duration;
        int filmId = rentalInfo.filmId;
        int staffId = rentalInfo.staffId;
        int storeId = rentalInfo.storeId;

        LOGGER.debug("Rent movie params:" + rentalInfo.toString() + "; ");
        if (filmId <= 0 || storeId <= 0 || customerId <= 0) {
            throw new GenericMIRestException("Invalid query params");
        }

        try (Scope scope = CommonUtils.startServerSpan(span, "rentMovie"))  {
            JSONObject result = mv.rentMovie(filmId, storeId, duration, customerId, staffId);
            return new RentMovieResult(result.getInt("inventory_id") , result.getInt("num_updates")
                    , result.getDouble("rent"));
        } catch (Exception e) {
            throw new GenericMIRestException(e.getMessage());
        }

    }

    @Override
    public ListStoreResult listStores(int filmId, Span span) throws GenericMIRestException, TException {
        ListStoreResult storeResult = new ListStoreResult();
        try (Scope scope = CommonUtils.startServerSpan(span, "listStores")) {
            JSONArray resultArr = mv.findAvailableStores(filmId);
            resultArr.forEach(jsonObj -> new StoreInfo(((JSONObject)jsonObj).getInt("store_id")));
            return storeResult;
        } catch (SQLException e) {
            throw new GenericMIRestException(e.getMessage());
        }
    }

    @Override
    public ReturnMovieResult returnMovie(ReturnInfo returnInfo, Span span)
        throws GenericMIRestException, TException {
        int inventoryId = returnInfo.inventoryId;
        int userId = returnInfo.userId;
        int staffId = returnInfo.staffId;
        double rent = returnInfo.rent;
        try (Scope scope = CommonUtils.startServerSpan(span, "returnMovie")) {
            LOGGER.debug(
                "ReturnMovie Params: " + inventoryId + ", " + userId + ", " + staffId + ", "
                    + rent);
            JSONObject jsonObject = mv.returnMovie(inventoryId, userId, staffId, rent);
            return new ReturnMovieResult(jsonObject.getInt("rental_id"),
                jsonObject.getInt("return_updates")
                , jsonObject.getInt("payment_updates"));
        }
    }
}
