import type {FDSCellRenderer} from '@liferay/js-api/data-set';

const fdsDateRenderer: FDSCellRenderer = ({value}) => {
	const element = document.createElement('div');

	const now = new Date().getTime();
	const date = new Date(value as string).getTime();
	let diff = (now-date)/(1000);

	// seconds
	if (diff < 5) {
		element.innerHTML = "Now"
		return element;
	}
	if (diff < 30) {
		element.innerHTML = diff.toFixed() + " seconds ago"
		return element;
	}
	if (diff < 60) {
		element.innerHTML = "Less than a minute ago"
		return element;
	}

	// minutes
	diff = diff/60;
	if (diff < 60) {
		element.innerHTML = diff.toFixed() + " minutes ago"
		return element;
	} 
	// hours
	diff = diff/60;
	if (diff < 24) {
		element.innerHTML = diff.toFixed() + " hours ago"
		return element;
	} 
	// days
	diff = diff/24;
	if (diff < 7) {
		element.innerHTML = diff.toFixed() + " days ago"
		return element;
	} 
	
	element.innerHTML = "More than a week ago"

	return element;
};

export default fdsDateRenderer;
