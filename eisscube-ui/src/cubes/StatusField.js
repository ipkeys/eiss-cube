import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import Typography from '@material-ui/core/Typography';
import { green, red } from '@material-ui/core/colors';

const StatusField = ({ record = {} }) => {
	let dur = '';
	if (record.online === true) {
		if (record.timeStarted !== 'undefined') {
			dur = moment.duration(moment() - moment(new Date(record.timeStarted))).humanize();
		}
		return (
			<Typography variant="body1">
				<span style={{ color: green[500] }}><b>ONLINE</b></span> { dur !== 'Invalid date' ? dur : '' }
			</Typography>
		);
	} else {
		if (record.lastPing !== 'undefined') {
			dur = moment.duration(moment() - moment(new Date(record.lastPing))).humanize();
		}
		return (
			<Typography variant="body1">
 				<span style={{ color: red[500] }}><b>OFFLINE</b></span> { dur !== 'Invalid date' ? dur : '' }
			</Typography>
		);
	}
};

StatusField.propTypes = {
    record: PropTypes.object
};

export default StatusField;
