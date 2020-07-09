import React from 'react';
import PropTypes from 'prop-types';
import Typography from '@material-ui/core/Typography';
import { green, red, amber, blue } from '@material-ui/core/colors';

const CommandStatusField = ({ record = {} }) => {
	if (record && record.status) {
		let color;
		switch(record.status) {
			case 'Pending':
				color = red[800];
				break;
			case 'Sending':
				color = amber[800];
				break;
			case 'Received':
				color = green[800];
				break;
			default:
				color = blue[800];
				break;
		}

		return (
			<Typography variant="body2">
				<span style={{ color: `${color}` }}><b>{record.status}</b></span>
			</Typography>
		);
	} else {
		return null;
	}
};

CommandStatusField.propTypes = {
    record: PropTypes.object
};

CommandStatusField.defaultProps = {
	addLabel: true,
	label: 'Status'
};

export default CommandStatusField;
