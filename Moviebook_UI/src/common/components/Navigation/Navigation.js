import React, { useState, useEffect, Fragment } from "react";
import { Link } from "react-router-dom";
import { connect } from "react-redux";
import UserAvatar from "react-user-avatar";
import { v4 as uuidv4 } from "uuid";
import {
	Dropdown,
	InputGroup,
	FormControl,
	Button,
	Modal,
	ListGroup,
} from "react-bootstrap";
import { authActions } from "../../actions";
import { moviebookActions } from "../../actions";
import CategoryViewer from "../../components/Categories/CategoryViewer";
import { transformCategoryToOptions } from "../../../utils/modifiers";
import Select from "react-dropdown-select";
import ICONS from "../../../utils/icons";
import "./Navigation.css";
import "./Categories.css";
import { history } from "../../helpers";

const CATEGORY_MODAL_ACTION = {
	DEFAULT: "DEFAULT",
	CREATE: "CREATE",
	EDIT: "EDIT",
};

const Navigation = (props) => {
	const {
		auth: {
			user: { username },
		},
		moviebook: {
			categoryList,
			categoryGroups,
			createCategoryStatus,
			selectedCategoryGroup,
			editCategoryStatus,
		},
		getMovieForSelectedCategoryGroup,
		clearCreateCategoryGroupMessage,
		clearEditCategoryGroupMessage,
		updateExistingCategoryGroup,
		updateSelectedCategory,
		createNewCategoryGroup,
		getAllCategoryGroups,
		deleteCategoryGroup,
		setDefaultCategory,
		getAllCategories,
		getMovieDetails,
		getMovieList,
		logout,
	} = props;

	const [keywords, setKeywords] = useState("");

	const [showEditModal, setShowEditModal] = useState(false);

	const [categoryNameAtCreate, setCategoryNameAtCreate] = useState("");

	const [categoryListAtCreate, setCategoryListAtCreate] = useState([]);

	const [categoryIdAtEdit, setCategoryIdAtEdit] = useState(null);

	const [categoryNameAtEdit, setCategoryNameAtEdit] = useState();

	const [categoryListAtEdit, setCategoryListAtEdit] = useState([]);

	const [selectedCategoryAction, setSelectedCategoryAction] = useState(
		CATEGORY_MODAL_ACTION.DEFAULT
	);

	const handleEditModalClose = () => {
		setShowEditModal(false);
	};

	const handleCreateCategoryClick = () => {
		setSelectedCategoryAction(CATEGORY_MODAL_ACTION.CREATE);
	};

	const handleBackClick = () => {
		setSelectedCategoryAction(CATEGORY_MODAL_ACTION.DEFAULT);
	};

	const handleLogoutClick = () => {
		history.push("/");
		logout();
	};

	const handleAppNameClick = () => {
		setDefaultCategory();
		getMovieList();
		history.push("/");
	};

	const handleCategoryEditClick = (categoryGroup) => {
		setSelectedCategoryAction(CATEGORY_MODAL_ACTION.EDIT);
		setCategoryIdAtEdit(categoryGroup.genre_group_id);
		setCategoryNameAtEdit(categoryGroup.name);
		setCategoryListAtEdit(
			transformCategoryToOptions(categoryGroup.categories)
		);
	};

	const handleEditCategorySaveClick = () => {
		const categories = categoryListAtEdit.map((category) => category.value);
		const updateCategoryGroup = {
			id: categoryIdAtEdit,
			name: categoryNameAtEdit,
			categories,
		};

		updateExistingCategoryGroup(updateCategoryGroup);
	};

	const handleCreateCategorySaveClick = () => {
		const categories = categoryListAtCreate.map(
			(category) => category.value
		);

		const newCategoryGroup = {
			// id: uuidv4(),
			name: categoryNameAtCreate,
			categories,
		};

		createNewCategoryGroup(newCategoryGroup);
	};

	const handleCategoryDeleteClick = (categoryGroup) => {
		deleteCategoryGroup(categoryGroup.genre_group_id);
	};

	const searchMovie = () => getMovieDetails(keywords);

	const handleCloseModal = () => {
		setSelectedCategoryAction(CATEGORY_MODAL_ACTION.DEFAULT);
		setShowEditModal(false);
		clearCreateCategoryGroupMessage();
		clearEditCategoryGroupMessage();
	};

	const changeSearchKey = (e) => setKeywords(e.target.value);

	const handleCategoryGroupItemClick = (category) => {
		updateSelectedCategory(category);
		getMovieForSelectedCategoryGroup(category);
	};

	const renderDefaultCategoryBody = () => {
		return (
			<Fragment>
				<CategoryViewer
					categoryGroups={categoryGroups}
					handleCategoryEditClick={handleCategoryEditClick}
					handleCategoryDeleteClick={handleCategoryDeleteClick}
				/>
				<Button
					className="categories-create-button"
					variant="outline-primary"
					onClick={handleCreateCategoryClick}
				>
					{ICONS.ADD}
					Create Category
				</Button>
			</Fragment>
		);
	};

	const renderCreateCategoryBody = () => {
		return (
			<Fragment>
				{createCategoryStatus.code === "INIT" ? (
					<Fragment>
						<InputGroup>
							<FormControl
								placeholder="Category Name"
								aria-label="Category Name"
								aria-describedby="basic-addon2"
								value={categoryNameAtCreate}
								onChange={(event) =>
									setCategoryNameAtCreate(event.target.value)
								}
							/>
						</InputGroup>
						<div className="categories-genre-container">
							<Select
								multi
								placeholder="Select Genre"
								options={transformCategoryToOptions(
									categoryList
								)}
								onChange={(values) =>
									setCategoryListAtCreate(values)
								}
								values={categoryListAtCreate}
							/>
						</div>
						<div className="categories-button-container">
							<Button
								className="categories-create-button"
								variant="outline-primary"
								onClick={handleBackClick}
							>
								{ICONS.BACK}
								Back
							</Button>
							<Button
								className="categories-create-button"
								variant="outline-success"
								onClick={handleCreateCategorySaveClick}
							>
								Create
							</Button>
						</div>
					</Fragment>
				) : (
					<div className="categories-create-message">
						{createCategoryStatus.message}
					</div>
				)}
			</Fragment>
		);
	};

	const renderEditCategoryBody = () => {
		return (
			<Fragment>
				{editCategoryStatus.code === "INIT" ? (
					<Fragment>
						<InputGroup>
							<FormControl
								placeholder="Category Name"
								aria-label="Category Name"
								aria-describedby="basic-addon2"
								value={categoryNameAtEdit}
								onChange={(event) =>
									setCategoryNameAtEdit(event.target.value)
								}
							/>
						</InputGroup>
						<div className="categories-genre-container">
							<Select
								multi
								values={categoryListAtEdit}
								placeholder="Select Genre"
								options={transformCategoryToOptions(
									categoryList
								)}
								onChange={(values) =>
									setCategoryListAtEdit(values)
								}
							/>
						</div>
						<div className="categories-button-container">
							<Button
								className="categories-create-button"
								variant="outline-primary"
								onClick={handleBackClick}
							>
								{ICONS.BACK}
								Back
							</Button>
							<Button
								className="categories-create-button"
								variant="outline-success"
								onClick={handleEditCategorySaveClick}
							>
								Save
							</Button>
						</div>
					</Fragment>
				) : (
					<div className="categories-create-message">
						{editCategoryStatus.message}
					</div>
				)}
			</Fragment>
		);
	};

	useEffect(() => {
		getAllCategoryGroups();
	}, [getAllCategoryGroups]);

	useEffect(() => {
		if (showEditModal) {
			getAllCategories();
		}
	}, [showEditModal]);

	return (
		<div>
			<nav className="navbar fixed-top navbar-dark bg-dark">
				<a
					onClick={handleAppNameClick}
					className="navbar-brand nav-brand-text"
				>
					MOVIEBOOK
				</a>
				<div className="nav-right-menu">
					<Dropdown>
						<Dropdown.Toggle
							variant="success"
							className="nav-dropdown-toggle"
						>
							{`Category Group: ${selectedCategoryGroup.name}`}
						</Dropdown.Toggle>

						<Dropdown.Menu>
							{categoryGroups.map((category) => (
								<Dropdown.Item
									onClick={() =>
										handleCategoryGroupItemClick(category)
									}
									key={category.genre_group_id}
								>
									{category.name}
								</Dropdown.Item>
							))}
						</Dropdown.Menu>
					</Dropdown>
					<form className="form-inline nav-search-input">
						<input
							value={keywords}
							onChange={changeSearchKey}
							className="form-control mr-sm-2"
							type="search"
							placeholder="Search"
							aria-label="Search"
						/>
						<Link to={"/search_results"}>
							<button
								onClick={searchMovie}
								className="btn btn-outline-success my-2 my-sm-0"
								type="submit"
							>
								Search
							</button>
						</Link>
						<Dropdown className="nav-dropdown-menu">
							<Dropdown.Toggle
								variant="success"
								className="nav-dropdown-toggle"
							>
								<UserAvatar size="24" name={username} />
							</Dropdown.Toggle>

							<Dropdown.Menu>
								<Dropdown.Item
									onClick={() => setShowEditModal(true)}
								>
									Manage Category Group
								</Dropdown.Item>
								<Dropdown.Item onClick={handleLogoutClick}>
									Logout
								</Dropdown.Item>
							</Dropdown.Menu>
						</Dropdown>
					</form>
				</div>
			</nav>
			<Modal
				size="lg"
				show={showEditModal}
				backdrop="static"
				onHide={handleEditModalClose}
			>
				<Modal.Header closeButton>
					<Modal.Title>
						{selectedCategoryAction ===
							CATEGORY_MODAL_ACTION.DEFAULT &&
							"Current Category Groups"}
						{selectedCategoryAction ===
							CATEGORY_MODAL_ACTION.CREATE &&
							"Create Category Group"}
						{selectedCategoryAction ===
							CATEGORY_MODAL_ACTION.EDIT && "Edit Category Group"}
					</Modal.Title>
				</Modal.Header>
				<Modal.Body>
					{selectedCategoryAction === CATEGORY_MODAL_ACTION.DEFAULT &&
						renderDefaultCategoryBody()}
					{selectedCategoryAction === CATEGORY_MODAL_ACTION.CREATE &&
						renderCreateCategoryBody()}
					{selectedCategoryAction === CATEGORY_MODAL_ACTION.EDIT &&
						renderEditCategoryBody()}
				</Modal.Body>
				<Modal.Footer>
					<Button variant="secondary" onClick={handleCloseModal}>
						Close
					</Button>
				</Modal.Footer>
			</Modal>
		</div>
	);
};

const mapStateToProps = (state) => ({
	auth: state.authentication,
	moviebook: state.moviebook,
});

const mapDispatchToProps = (dispatch) => ({
	logout: () => dispatch(authActions.logout()),

	getMovieList: () => dispatch(moviebookActions.getMovieList()),

	getMovieDetails: (keywords) =>
		dispatch(moviebookActions.getMovieDetails(keywords)),

	updateSelectedCategory: (genreGroupId) =>
		dispatch(moviebookActions.updateSelectedCategory(genreGroupId)),

	setDefaultCategory: () => dispatch(moviebookActions.setDefaultCategory()),

	getAllCategoryGroups: () =>
		dispatch(moviebookActions.getAllCategoryGroups()),

	getAllCategories: () => dispatch(moviebookActions.getAllCategories()),

	getMovieForSelectedCategoryGroup: (selectedCategoryGroup) =>
		dispatch(
			moviebookActions.getMovieForSelectedCategoryGroup(
				selectedCategoryGroup
			)
		),
	createNewCategoryGroup: (newCategoryGroup) =>
		dispatch(moviebookActions.createNewCategoryGroup(newCategoryGroup)),

	clearCreateCategoryGroupMessage: () =>
		dispatch(moviebookActions.clearCreateCategoryGroupMessage()),

	clearEditCategoryGroupMessage: () =>
		dispatch(moviebookActions.clearEditCategoryGroupMessage()),

	updateExistingCategoryGroup: (updateCategoryGroup) =>
		dispatch(
			moviebookActions.updateExistingCategoryGroup(updateCategoryGroup)
		),
	deleteCategoryGroup: (genreGroupId) =>
		dispatch(moviebookActions.deleteCategoryGroup(genreGroupId)),
});

export default connect(mapStateToProps, mapDispatchToProps)(Navigation);
