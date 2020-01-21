import config from '../config';
import axios from 'axios';

const getMovieList = async (keywords) => {
    const url = `${config.apiBaseUrl}/listmovies?filmName=${keywords}`;
    const requestHeaders = {
        headers: {

        }
    };
    let data;

    await axios.get(url, requestHeaders)
        .then(response => {
            data = response.data;
        })
        .catch(function(error){
            alert(error);
            throw (error.response);
        });

    return data;
};

const rentMovie = async (filmId, storeId, duration, custId, staffId) => {
    const url = `${config.apiBaseUrl}/rentmovie`;
    const headers = {
        'Access-Control-Allow-Origin': '*',
        "Content-Type": "application/x-www-form-urlencoded",
    };
    await axios.post(url, {
        "filmId": filmId,
        "storeId": storeId,
        "duration": duration,
        "customerId": custId,
        "staffId": staffId
    },{
        headers: headers
    }).then(function(response){
        return response;
    }).catch(function(error){
        throw (error.response);
    });
};

const returnMovie = async (inventoryId, rent, userId, staffId) => {
    const url = `${config.apiBaseUrl}/returnmovie`;
    const headers = {
        'Access-Control-Allow-Origin': '*',
        "Content-Type": "application/x-www-form-urlencoded",
    };
    await axios.post(url, {
        "inventoryId": inventoryId,
        "rent": rent,
        "userId": userId,
        "staffId": staffId
    },{
        headers: headers
    }).then(function(response){
        return response;
    }).catch(function(error){
        throw (error.response);
    });
};

export const moviebookService = {
    getMovieList,
    rentMovie,
    returnMovie
};
