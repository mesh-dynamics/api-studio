namespace java com.cubeiosample.webservices.thirft

/*
bool: boolean
binary: byte[]
byte: byte
i16: short
i32: int
i64: long
double: double
string: String
list<t1>: List<t1>
set<t1>: Set<t1>
map<t1,t2>: Map<t1, t2>
*/

enum Color {
    BLUE,
    RED,
    GREEN,
    BLACK
}

/**
* {
    "display_actors": [
      "NOLTE,JAYNE",
      "WILLIS,HUMPHREY",
      "MARX,ELVIS"
    ],
    "film_id": 23,
    "title": "ANACONDA CONFESSIONS",
    "film_counts": [
      "22",
      "26",
      "34",
      "19",
      "26"
    ],
    "timestamp": 572790039541041,
    "book_info": {
      "reviews": [
        {
          "reviewer": "Reviewer1",
          "text": "An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!"
        },
        {
          "reviewer": "Reviewer2",
          "text": "Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare."
        }
      ],
      "ratings": {
        "Reviewer2": 4,
        "Reviewer1": 5
      },
      "details": {
        "pages": 200,
        "year": 1595,
        "author": "William Shakespeare",
        "ISBN-13": "123-1234567890",
        "publisher": "PublisherA",
        "ISBN-10": "1234567890",
        "language": "English",
        "id": 23,
        "type": "paperback"
      }
    }
  }
**/

struct Details {
    1: i32 pages;
    2: i16 year;
    3: string author;
    4: string isbn_13;
    5: string publisher;
    6: string isbn_10;
    7: string language;
    8: string type;
    9: i32 id;
}

struct Rating {
    1: string reviewer;
    2: i16 stars;
}

struct Review {
    1: string reviewer;
    2: string text;
}

struct BookInfo {
    1: list<Review> reviews;
    2: list<Rating> ratings;
    3: Details details;
    //3: i32 id;
}

struct MovieInfo {
    1: list<string> actorsLastNames;
    2: list<string> actorsFirstNames;
    3: list<string> displayActors;
    4: i32 filmId;
    5: string title;
    6: list<i32> filmCounts;
    7: list<string> filmCountsString;
    8: i64 timestamp;
    9: optional BookInfo bookInfo;
}

struct ListMovieResult {
    1: list<MovieInfo> movieInfoList;
}

struct RentalInfo {
    1: i32 filmId;
    2: i32 storeId;
    3: i32 customerId;
    4: i32 duration;
    5: i32 staffId;
}

struct RentMovieResult {
    1: i32 inventoryId;
    2: i32 numUpdates;
    3: double rent;
}

struct StoreInfo {
    1: i32 storeId;
}

struct ListStoreResult {
    1: list<StoreInfo> storeInfoList;
}

exception GenericMIRestException {
    1: string message;
}

struct ReturnInfo {
     1: i32 inventoryId;
     2: i32 userId;
     3: i32 staffId;
     4: double rent;
}

struct ReturnMovieResult {
     1: i32 rentalId;
     2: i32 returnUpdates;
     3: i32 paymentUpdates;
}

service MIRest {

    bool healthCheck(),

    ListMovieResult listMovies(1: string filmName, 2: string keyWord, 3: string actor)
        throws (1: GenericMIRestException genericException),

    RentMovieResult rentMovie(1: RentalInfo rentalInfo)
        throws (1: GenericMIRestException genericException);

    ListStoreResult listStores(1: i32 filmId) throws (1: GenericMIRestException genericException);

    ReturnMovieResult returnMovie(1: ReturnInfo returnInfo)
        throws (1: GenericMIRestException genericException);

}