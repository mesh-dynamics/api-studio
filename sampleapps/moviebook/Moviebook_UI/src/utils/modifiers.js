const transformCategoryToOptions = (categoryList) => {
	return categoryList.map((category) => ({
		id: category.category_id,
		label: category.name,
		value: category.name,
	}));
};

const transformOptionToCategory = (option) => ({
	category_id: option.id,
	name: option.value,
});

export { transformCategoryToOptions, transformOptionToCategory };
