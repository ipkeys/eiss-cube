import PropTypes from 'prop-types';
import { useRecordContext } from 'react-admin';
import Typography from '@mui/material/Typography';
import { green, red, amber, blue } from '@mui/material/colors';

const CommandStatusField = (props: any) => {
    const record = useRecordContext(props);

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
			<Typography variant='body2'>
				<span style={{ color: `${color}` }}><b>{record.status}</b></span>
			</Typography>
		);
	} else {
		return null;
	}
};

CommandStatusField.propTypes = {
	addLabel: PropTypes.bool,
	label: PropTypes.string,
	record: PropTypes.object
};

CommandStatusField.defaultProps = {
	addLabel: true,
	label: 'Status'
};

export default CommandStatusField;
