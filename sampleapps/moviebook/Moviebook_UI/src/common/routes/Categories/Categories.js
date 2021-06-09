import React, { useState, Fragment } from "react";
import {
	InputGroup,
	FormControl,
	Button,
	Modal,
	ListGroup,
} from "react-bootstrap";
import CategoryViewer from "../../components/Categories/CategoryViewer";
import ICONS from "../../../utils/icons";
import "./Categories.css";

const CATEGORY_MODAL_ACTION = {
	DEFAULT: "DEFAULT",
	CREATE: "CREATE",
	EDIT: "EDIT",
};

const Categories = () => {
	const [showEditModal, setShowEditModal] = useState(false);

	const [selectedCategoryAction, setSelectedCategoryAction] = useState(
		CATEGORY_MODAL_ACTION.DEFAULT
	);

	const handleEditModalClose = () => setShowEditModal(false);

	const handleCreateCategoryClick = () => {
		setSelectedCategoryAction(CATEGORY_MODAL_ACTION.CREATE);
	};

	const handleBackClick = () => {
		setSelectedCategoryAction(CATEGORY_MODAL_ACTION.DEFAULT);
	};

	const handleCategoryEditClick = () => {
		setSelectedCategoryAction(CATEGORY_MODAL_ACTION.EDIT);
	};

	const handleCategoryDeleteClick = () => {};

	const renderDefaultCategoryBody = () => {
		return (
			<Fragment>
				<CategoryViewer
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
				<InputGroup>
					<FormControl
						placeholder="Category Name"
						aria-label="Category Name"
						aria-describedby="basic-addon2"
					/>
				</InputGroup>
				<Button
					className="categories-create-button"
					variant="outline-primary"
					onClick={handleBackClick}
				>
					{ICONS.BACK}
					Back
				</Button>
			</Fragment>
		);
	};

	const renderEditCategoryBody = () => {
		return (
			<Fragment>
				<Button
					className="categories-create-button"
					variant="outline-primary"
					onClick={handleBackClick}
				>
					{ICONS.BACK}
					Back
				</Button>
			</Fragment>
		);
	};

	return (
		<div className="categories-root">
			<InputGroup>
				<FormControl
					placeholder="Search Categories"
					aria-label="Search Categories"
					aria-describedby="basic-addon2"
				/>
				<InputGroup.Append>
					<Button variant="outline-secondary">Search</Button>
					<Button
						variant="outline-secondary"
						onClick={() => setShowEditModal(true)}
					>
						My Categories
					</Button>
				</InputGroup.Append>
			</InputGroup>
			<Modal size="lg" show={showEditModal} onHide={handleEditModalClose}>
				<Modal.Header closeButton>
					<Modal.Title>
						{selectedCategoryAction ===
							CATEGORY_MODAL_ACTION.DEFAULT &&
							"Your Custom Categories"}
						{selectedCategoryAction ===
							CATEGORY_MODAL_ACTION.CREATE && "Create A Category"}
						{selectedCategoryAction ===
							CATEGORY_MODAL_ACTION.EDIT && "Edit Category"}
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
					{/* <Button variant="secondary" onClick={() => {}}>
            Cancel
          </Button>
          <Button variant="primary" onClick={() => {}}>
            Update
          </Button> */}
				</Modal.Footer>
			</Modal>
		</div>
	);
};

export default Categories;
