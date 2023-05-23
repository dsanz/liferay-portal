import type {FDSCellRenderer} from '@liferay/js-api/data-set';

const setMessage = (message) => {
	const element = document.createElement('div');
	element.innerHTML = message;
	return element;
}

const fdsDateRenderer: FDSCellRenderer = ({value}) => {
	const now = new Date().getTime();
	const date = new Date(value as string).getTime();
	let diff = (now-date)/(1000);

	// seconds
	if (diff < 5)  { return setMessage("Now") }
	if (diff < 20) { return setMessage(diff.toFixed() + " seconds ago") }
	if (diff < 60) { return setMessage("Less than a minute ago") }
	// minutes
	diff = diff/60;
	if (diff < 60) { return setMessage(diff.toFixed() + " minute(s) ago") } 
	// hours
	diff = diff/60;
	if (diff < 24) { return setMessage(diff.toFixed() + " hour(s) ago") } 
	// days
	diff = diff/24;
	if (diff < 7) {	 return setMessage(diff.toFixed() + " day(s) ago") } 

	return setMessage("Long ago");
};

export default fdsDateRenderer;
