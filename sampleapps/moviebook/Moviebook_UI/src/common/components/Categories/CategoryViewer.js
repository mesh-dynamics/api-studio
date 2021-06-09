import React, { Component } from "react";
import PropTypes from "prop-types";
import { ListGroup } from "react-bootstrap";
import ICONS from "../../../utils/icons";

class CategoryEditor extends Component {
	render() {
		const {
			handleCategoryDeleteClick,
			handleCategoryEditClick,
			categoryGroups,
		} = this.props;
		return (
			<ListGroup>
				{categoryGroups
					.slice(1, categoryGroups.length)
					.map((categoryGroup) => (
						<ListGroup.Item
							className="categories-item-container"
							key={categoryGroup.genre_group_id}
						>
							<div className="categories-item-wrapper">
								{categoryGroup.name}
								<div className="categories-item-icon-wrapper">
									<span
										onClick={() =>
											handleCategoryEditClick(
												categoryGroup
											)
										}
									>
										{ICONS.EDIT}
									</span>
									<span
										onClick={() =>
											handleCategoryDeleteClick(
												categoryGroup
											)
										}
									>
										{ICONS.DELETE}
									</span>
								</div>
							</div>
						</ListGroup.Item>
					))}
			</ListGroup>
		);
	}
}

CategoryEditor.propTypes = {};

export default CategoryEditor;
