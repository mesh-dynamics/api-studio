import React, { Component } from "react";
import { connect } from "react-redux";
import Modal from "react-bootstrap/Modal";
import _ from "lodash";
import "./MovieDetails.css";
import config from "../../config";
import axios from "axios";
import Button from "react-bootstrap/Button";

class MovieDetails extends Component {
	constructor(props) {
		super(props);
		this.state = {
			movieDetails: null,
			stores: null,
			showRentModal: false,
			selectedStore: null,
			rentNumDays: 2,
			showRentCompleteModal: false,
			rentMovieSuccess: false,
			rentMovieInProgress: false,
		};
	}

	componentDidMount() {
		let urlParameters = _.chain(window.location.search)
			.replace("?", "")
			.split("&")
			.map(_.partial(_.split, _, "=", 2))
			.fromPairs()
			.value();

		this.getMovieDetails(urlParameters.film);
	}

	getStoresForMovie = () => {
		const {
			user: { token },
		} = this.props;

		const headers = {
			Authorization: `${token}`,
		};
		const url = `${config.apiBaseUrl}/liststores?filmId=${this.state.movieDetails.film_id}`;

		axios
			.get(url, { headers })
			.then((response) => {
				this.setState({ stores: response.data });
			})
			.catch(function (error) {
				alert(error);
				throw error.response;
			});
	};

	getMovieDetails = (film) => {
		const url = `${config.apiBaseUrl}/listmovies?filmName=${film}`;
		const {
			user: { token },
		} = this.props;

		const headers = {
			Authorization: `${token}`,
		};

		axios
			.get(url, { headers })
			.then((response) => {
				this.setState({ movieDetails: response.data[0] });
				setTimeout(() => {
					this.getStoresForMovie();
				});
			})
			.catch(function (error) {
				alert(error);
				throw error.response;
			});
	};

	hideRentModal = () => {
		this.setState({
			showRentModal: false,
			selectedStore: null,
			showRentCompleteModal: false,
		});
	};

	openRentModal = (ev, store) => {
		console.log(store);
		this.setState({ showRentModal: true, selectedStore: store });
	};

	changeNumDays = (ev) => {
		this.setState({ rentNumDays: ev.target.value });
	};

	getAvgRatings = (revs) => {
		let sum = 0;
		for (const i in revs) {
			if (revs[i].rating) sum += revs[i].rating.stars;
		}

		let avg = (sum / revs.length).toFixed(1);

		return avg + " STARS";
	};

	rentMovie = () => {
		const {
			user: { token },
		} = this.props;

		this.setState({ rentMovieInProgress: true });
		const url = `${config.apiBaseUrl}/rentmovie`;
		const headers = {
			// "Access-Control-Allow-Origin": "*",
			"Content-Type": "application/json",
			"cache-control": "no-cache",
			Authorization: `${token}`,
		};
		axios
			.post(
				url,
				{
					filmId: this.state.movieDetails.film_id,
					storeId: this.state.selectedStore.store_id,
					duration: this.state.rentNumDays,
					customerId: 200,
					staffId: 1,
				},
				{ headers }
			)
			.then((response) => {
				const { inventory_id } = response.data;
				if (inventory_id > 0) {
					this.setState({
						rentMovieSuccess: true,
						showRentCompleteModal: true,
						rentMovieInProgress: false,
					});
				} else {
					this.setState({
						rentMovieSuccess: false,
						showRentCompleteModal: true,
						rentMovieInProgress: false,
					});
				}
			})
			.catch((error) => {
				console.log("Error", error);
				this.setState({
					rentMovieSuccess: false,
					showRentCompleteModal: true,
					rentMovieInProgress: false,
				});
				throw error.response;
			});
	};

	render() {
		const {
			movieDetails,
			stores,
			selectedStore,
			rentNumDays,
			showRentModal,
			rentMovieInProgress,
			showRentCompleteModal,
			rentMovieSuccess,
		} = this.state;

		return (
			<div className="mov-detail">
				<div className="title-bar">
					<div className="movie-title">
						{movieDetails ? (
							<div>
								{movieDetails.title}
								<span className="pull-right small">
									{movieDetails.book_info &&
										this.getAvgRatings(
											movieDetails.book_info.reviews
												.reviews
										)}
								</span>
							</div>
						) : (
							"loading..."
						)}
					</div>
				</div>

				{movieDetails ? (
					<React.Fragment>
						<div className="row margin-bottom-10">
							<div className="cast col-md-6 text-center">
								<h5>Cast</h5>
								{movieDetails.display_actors.map((actor) => (
									<div key={actor}>{actor}</div>
								))}
							</div>

							{movieDetails.book_info && (
								<div className="cast col-md-6 text-center">
									<h5>Details</h5>
									<div>
										Author:{" "}
										{movieDetails.book_info.details.author}
									</div>
									<div>
										Pages:{" "}
										{movieDetails.book_info.details.pages}
									</div>
									<div>
										Year:{" "}
										{movieDetails.book_info.details.year}
									</div>
									<div>
										Language:{" "}
										{
											movieDetails.book_info.details
												.language
										}
									</div>
								</div>
							)}
						</div>

						{movieDetails.book_info && (
							<div className="reviews">
								<h5>
									Reviews(
									{
										movieDetails.book_info.reviews.reviews
											.length
									}
									)
								</h5>
								{movieDetails.book_info.reviews.reviews.map(
									(review) => (
										<div
											key={review.reviewer}
											className="review"
										>
											<h6>
												{review.reviewer} (
												{review.rating
													? review.rating.stars
													: 0}
												/5)
											</h6>
											<p>{review.text}</p>
										</div>
									)
								)}
							</div>
						)}
					</React.Fragment>
				) : (
					<React.Fragment></React.Fragment>
				)}

				{stores ? (
					<React.Fragment>
						<div className="reviews">
							<h5>Stores</h5>
							{stores.map((store) => (
								<div className="review" key={store.store_id}>
									<h6>
										{"Store " + store.store_id}
										<span
											onClick={(ev) =>
												this.openRentModal(ev, store)
											}
											className="mb-btn pull-right"
										>
											Rent
										</span>
									</h6>
								</div>
							))}
						</div>

						<Modal onHide={this.hideRentModal} show={showRentModal}>
							<Modal.Header closeButton>
								<Modal.Title>
									{selectedStore
										? "Store " + selectedStore.store_id
										: ""}
								</Modal.Title>
							</Modal.Header>

							<Modal.Body>
								<p className="text-center">
									Number Of Days:{" "}
									<input
										style={{
											width: "50px",
											textAlign: "center",
										}}
										onChange={this.changeNumDays}
										value={rentNumDays}
										type="number"
									/>
								</p>
							</Modal.Body>

							<Modal.Footer>
								{!rentMovieInProgress && (
									<Button
										onClick={this.hideRentModal}
										variant="secondary"
									>
										Cancel
									</Button>
								)}

								<Button
									disabled={rentMovieInProgress}
									onClick={this.rentMovie}
									variant="primary"
								>
									{rentMovieInProgress ? (
										<i
											className="fa fa-spinner fa-spin"
											style={{ "font-size": "24px" }}
										/>
									) : (
										"Rent"
									)}
								</Button>
							</Modal.Footer>
						</Modal>

						<Modal show={showRentCompleteModal}>
							<Modal.Header>
								<Modal.Title>Rent Movie</Modal.Title>
							</Modal.Header>
							<Modal.Body>
								{rentMovieSuccess ? (
									<p>Movie rented successfully</p>
								) : (
									<p>Unable to rent movie</p>
								)}
							</Modal.Body>
							<Modal.Footer>
								<Button
									onClick={this.hideRentModal}
									variant="secondary"
								>
									Close
								</Button>
							</Modal.Footer>
						</Modal>
					</React.Fragment>
				) : (
					<React.Fragment></React.Fragment>
				)}
			</div>
		);
	}
}

const mapStateToProps = (state) => ({
	user: state.authentication.user,
});

export default connect(mapStateToProps)(MovieDetails);
